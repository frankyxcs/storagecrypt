/*
 *  Copyright Pierre Sagne (12 december 2014)
 *
 * petrus.dev.fr@gmail.com
 *
 * This software is a computer program whose purpose is to encrypt and
 * synchronize files on the cloud.
 *
 * This software is governed by the CeCILL license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 *
 */

package fr.petrus.lib.core.processes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import fr.petrus.lib.core.Constants;
import fr.petrus.lib.core.EncryptedDocumentMetadata;
import fr.petrus.lib.core.EncryptedDocuments;
import fr.petrus.lib.core.ParentNotFoundException;
import fr.petrus.lib.core.StorageCryptException;
import fr.petrus.lib.core.SyncAction;
import fr.petrus.lib.core.cloud.Account;
import fr.petrus.lib.core.cloud.Accounts;
import fr.petrus.lib.core.cloud.RemoteDocument;
import fr.petrus.lib.core.cloud.RemoteStorage;
import fr.petrus.lib.core.cloud.exceptions.RemoteException;
import fr.petrus.lib.core.cloud.RemoteChange;
import fr.petrus.lib.core.cloud.RemoteChanges;
import fr.petrus.lib.core.EncryptedDocument;
import fr.petrus.lib.core.State;
import fr.petrus.lib.core.crypto.Crypto;
import fr.petrus.lib.core.crypto.KeyManager;
import fr.petrus.lib.core.db.exceptions.DatabaseConnectionClosedException;
import fr.petrus.lib.core.processes.results.BaseProcessResults;
import fr.petrus.lib.core.processes.results.FailedResult;
import fr.petrus.lib.core.result.ProcessProgressAdapter;
import fr.petrus.lib.core.result.ProgressListener;
import fr.petrus.lib.core.network.Network;
import fr.petrus.lib.core.i18n.TextI18n;

/**
 * The {@code Process} which handles remote changes synchronization.
 *
 * <p>Its purpose is to scan a {@code RemoteStorage} and pull the changes (creations, modifications
 * and deletions) on of its {@code RemoteDocument}s
 *
 * @author Pierre Sagne
 * @since 29.12.2014
 */
public class ChangesSyncProcess extends AbstractProcess<ChangesSyncProcess.Results> {

    private static Logger LOG = LoggerFactory.getLogger(ChangesSyncProcess.class);

    /**
     * The interface used by this process for communicating with its caller.
     */
    public interface SyncActionListener {
        /**
         * The method called when the synchronization of the given {@code rootEncryptedDocument} is
         * finished.
         *
         * @param rootEncryptedDocument the root encrypted document which was synchronized
         */
        void onChangesSyncDone(EncryptedDocument rootEncryptedDocument);
    }

    /**
     * The {@code ProcessResults} implementation for this particular {@code Process} implementation.
     */
    public static class Results extends BaseProcessResults<EncryptedDocument, String> {
        /**
         * A boolean indicating if a report should be shown to the user on completion.
         */
        public boolean showResults = false;

        /**
         * Creates a new {@code Results} instance, providing its dependencies.
         *
         * @param textI18n a {@code textI18n} instance
         */
        public Results(TextI18n textI18n) {
            super (textI18n, true, false, true);
        }

        @Override
        public int getResultsColumnsCount(ResultsType resultsType) {
            if (null!=resultsType) {
                switch (resultsType) {
                    case Success:
                        return 1;
                    case Skipped:
                        return 0;
                    case Errors:
                        return 2;
                }
            }
            return 0;
        }

        @Override
        public String[] getResultColumns(ResultsType resultsType, int i) {
            String[] result;
            if (null==resultsType) {
                result = new String[0];
            } else {
                switch (resultsType) {
                    case Success:
                        try {
                            result = new String[] { success.get(i).logicalPath() };
                        } catch (ParentNotFoundException e) {
                            LOG.error("Parent not found", e);
                            result = new String[] { success.get(i).getDisplayName() };
                        } catch (DatabaseConnectionClosedException e) {
                            LOG.error("Database is closed", e);
                            result = new String[] { success.get(i).getDisplayName() };
                        }
                        break;
                    case Skipped:
                        result = new String[0];
                        break;
                    case Errors:
                        result = new String[]{
                                errors.get(i).getElement(),
                                textI18n.getExceptionDescription(errors.get(i).getException())
                        };
                        break;
                    default:
                        result = new String[0];
                }
            }
            return result;
        }
    }

    /**
     * This class is used to send particular details on the process state.
     */
    public static class SyncResult {
        /**
         * An enumeration describing what happened with the processed element.
         */
        public enum Result {
            /**
             * The processed element was synchronized.
             */
            Synced,

            /**
             * The processed element was ignored.
             */
            Ignored
        }

        /**
         * The status of the processed document.
         */
        public Result result;

        /**
         * The processed document.
         */
        public EncryptedDocument encryptedDocument;

        /**
         * Creates a new {@code SyncResult}.
         *
         * @param result the result type
         */
        public SyncResult(Result result) {
            this(result, null);
        }

        /**
         * Creates a new {@code SyncResult}.
         *
         * @param result            the result type
         * @param encryptedDocument the processed encrypted document
         */
        public SyncResult(Result result, EncryptedDocument encryptedDocument) {
            this.result = result;
            this.encryptedDocument = encryptedDocument;
        }
    }

    private Crypto crypto;
    private KeyManager keyManager;
    private Network network;
    private Accounts accounts;
    private EncryptedDocuments encryptedDocuments;
    private List<EncryptedDocument> successfulSyncs = new ArrayList<>();
    private LinkedHashMap<String, FailedResult<String>> failedSyncs = new LinkedHashMap<>();
    private ProgressListener progressListener;
    private SyncActionListener syncActionListener;

    /**
     * Creates a new {@code ChangesSyncProcess}, providing its dependencies.
     *
     * @param crypto             a {@code Crypto} instance
     * @param keyManager         a {@code KeyManager} instance
     * @param textI18n           a {@code TextI18n} instance
     * @param network            a {@code Network} instance
     * @param accounts           an {@code Accounts} instance
     * @param encryptedDocuments an {@code EncryptedDocuments} instance
     */
    public ChangesSyncProcess(Crypto crypto, KeyManager keyManager, TextI18n textI18n, Network network,
                              Accounts accounts, EncryptedDocuments encryptedDocuments) {
        super(new Results(textI18n));
        this.crypto = crypto;
        this.keyManager = keyManager;
        this.network = network;
        this.accounts = accounts;
        this.encryptedDocuments = encryptedDocuments;
        progressListener = null;
        syncActionListener = null;
    }

    /**
     * Sets the {@code ProgressListener} which this process will report its progress to.
     *
     * @param progressListener the {@code ProgressListener} which this process will report its progress to
     */
    public void setProgressListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    /**
     * Sets the {@code SyncActionListener} which this process will report its progress to.
     *
     * @param syncActionListener the {@code SyncActionListener} which this process will report its progress to
     */
    public void setSyncActionListener(SyncActionListener syncActionListener) {
        this.syncActionListener = syncActionListener;
    }

    /**
     * Tells this process that the results should be shown to the user after completion.
     */
    public void showResults() {
        getResults().showResults = true;
    }

    /**
     * Marks all the accounts so that they will be synchronized by this process.
     *
     * @param accounts the accounts
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public static void syncAll(Accounts accounts) throws DatabaseConnectionClosedException {
        sync(accounts.allAccounts());
    }

    /**
     * Marks the given {@code accounts} so that they will be synchronized by this process.
     *
     * @param accounts the accounts to be synchronized
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public static void sync(List<Account> accounts) throws DatabaseConnectionClosedException {
        for (Account account : accounts) {
            sync(account);
        }
    }

    /**
     * Marks the given {@code account} so that it will be synchronized by this process.
     *
     * @param account the account to be synchronized
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public static void sync(Account account) throws DatabaseConnectionClosedException {
        if (null == account.getChangesSyncState() || State.Done == account.getChangesSyncState()) {
            account.updateChangesSyncState(State.Planned);
        }
    }

    /**
     * Runs this process, synchronizing all the accounts marked for planned synchronization.
     *
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    public void run() throws DatabaseConnectionClosedException {
        try {
            start();
            cleanupSyncStates();
            while (network.isConnected()) {
                List<Account> accountList = accounts.accountsWithChangesSyncState(State.Planned);
                if (null == accountList || accountList.isEmpty()) {
                    break;
                }
                if (null != progressListener) {
                    progressListener.onSetMax(0, accountList.size());
                }
                for (int i = 0; i < accountList.size(); i++) {
                    Account account = accountList.get(i);
                    account.refresh();
                    if (null != progressListener) {
                        progressListener.onMessage(0, account.storageText());
                        progressListener.onMessage(1, "");
                        progressListener.onProgress(0, i);
                        progressListener.onProgress(1, 0);
                        progressListener.onSetMax(1, 0);
                    }
                    pauseIfNeeded();
                    if (isCanceled()) {
                        break;
                    }
                    syncChanges(account);
                    syncQuota(account);
                }
                pauseIfNeeded();
                if (isCanceled()) {
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    LOG.debug("ChangesSyncProcess interrupted, exiting", e);
                    break;
                }
            }
        } finally {
            cleanupSyncStates();
            getResults().addResults(successfulSyncs, failedSyncs.values());
        }
    }

    /**
     * This method is called when finishing this process execution, to mark the accounts which
     * remain for some reason on the "Running" state, as "Done"
     *
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    private void cleanupSyncStates() throws DatabaseConnectionClosedException {
        List<Account> accountsList = accounts.accountsWithChangesSyncState(State.Running);
        for (Account account : accountsList) {
            account.updateChangesSyncState(State.Done);
        }
    }

    /**
     * Synchronizes the quota information of the given {@code account}
     *
     * @param account the account which quota to synchronize
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    private void syncQuota(Account account) throws DatabaseConnectionClosedException {
        LOG.debug("Refreshing account quota for {}", account.storageText());
        try {
            account.refreshQuota();
        } catch (RemoteException e) {
            LOG.error("Failed to refresh account quota for {}", account.storageText(), e);
        }
    }

    /**
     * Synchronizes the changes for the given {@code account}
     *
     * @param account the {@code Account} to synchronize
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    private void syncChanges(Account account) throws DatabaseConnectionClosedException {
        String lastChangeId = null;
        account.updateChangesSyncState(State.Running);
        EncryptedDocument rootEncryptedDocument = encryptedDocuments.root(account.getStorageType(), account);
        if (null != syncActionListener) {
            syncActionListener.onChangesSyncDone(rootEncryptedDocument);
        }
        try {
            if (rootEncryptedDocument.isRoot() && !rootEncryptedDocument.isUnsynchronizedRoot()) {
                rootEncryptedDocument.checkRemoteRoot();
                account.refresh();
                String startChangeId = account.getLastRemoteChangeId();
                LOG.debug("Sync since last change id : {}", startChangeId);
                RemoteStorage storage = account.getRemoteStorage();
                if (null != storage) {
                    RemoteChanges changes = storage.changes(
                            rootEncryptedDocument.getBackStorageAccount().getAccountName(),
                            startChangeId,
                            new ProcessProgressAdapter() {
                                @Override
                                public boolean isCanceled() {
                                    return ChangesSyncProcess.this.isCanceled();
                                }

                                @Override
                                public void pauseIfNeeded() {
                                    ChangesSyncProcess.this.pauseIfNeeded();
                                }

                                @Override
                                public void onProgress(int i, int progress) {
                                    if (null != progressListener) {
                                        if (0 == i) {
                                            progressListener.onSetMax(1, progress);
                                        }
                                    }
                                }
                            });
                    if (null != changes) {
                        if (null != progressListener) {
                            progressListener.onSetMax(1, changes.getChanges().size());
                        }
                        Map<String, RemoteChange> changesToProcess = changes.getChanges();
                        Map<String, RemoteChange> folderChanges = changes.getFolderChanges();
                        int i = 0;
                        while (!changesToProcess.isEmpty()) {
                            for (Iterator<Map.Entry<String, RemoteChange>>
                                 it = changesToProcess.entrySet().iterator();
                                 it.hasNext(); ) {
                                Map.Entry<String, RemoteChange> changeEntry = it.next();
                                if (null != progressListener) {
                                    progressListener.onProgress(1, i);
                                }
                                pauseIfNeeded();
                                if (isCanceled()) {
                                    return;
                                }
                                LOG.debug("Change {} on file with id {} : ", i, changeEntry.getKey());
                                RemoteChange change = changeEntry.getValue();
                                LOG.debug(" - documentId = {}", change.getDocumentId());
                                if (change.isDeleted()) {
                                    LOG.debug(" - deleted");
                                }
                                if (null != change.getDocument()) {
                                    LOG.debug(" - document = {}", change.getDocument().getName());
                                }
                                try {
                                    SyncResult syncResult =
                                            syncChange(rootEncryptedDocument, folderChanges, change);
                                    switch (syncResult.result) {
                                        case Synced:
                                            successfulSyncs.add(syncResult.encryptedDocument);
                                            it.remove();
                                            i++;
                                            break;
                                        case Ignored:
                                            it.remove();
                                            i++;
                                            break;
                                    }
                                } catch (StorageCryptException e) {
                                    if (e.getReason() != StorageCryptException.Reason.ParentNotFound) {
                                        if (change.isDeleted()) {
                                            failedSyncs.put(change.getDocumentId(),
                                                    new FailedResult<>(account.storageText() + " : - " + change.getDocumentId(), e));
                                        } else {
                                            failedSyncs.put(change.getDocumentId(),
                                                    new FailedResult<>(account.storageText() + " : + " + change.getDocument().getId(), e));
                                        }
                                        it.remove();
                                        i++;
                                    }
                                }
                            }
                        }
                        if (failedSyncs.isEmpty() && null != changes.getLastChangeId()) {
                            lastChangeId = changes.getLastChangeId();
                            LOG.debug("Last Change Id : {}", lastChangeId);
                        }
                    }
                    if (null != lastChangeId) {
                        account.setLastRemoteChangeId(lastChangeId);
                        account.update();
                    }
                }
            }
        } catch (StorageCryptException e) {
            LOG.debug("Error while getting changes", e);
        } catch (RemoteException e) {
            LOG.debug("Error while getting changes", e);
        }
        account.updateChangesSyncState(State.Done);
        if (null != syncActionListener) {
            syncActionListener.onChangesSyncDone(rootEncryptedDocument);
        }
    }

    /**
     * Performs the change described by the given {@code change}
     *
     * @param rootEncryptedDocument the root {@code EncryptedDocument} containing the {@code change}
     * @param folderChanges         the registered changes for the folders, used to find the parents of
     *                              the given {@code change}
     * @param change                the change to process
     * @throws StorageCryptException if an error occurs when accessing a {@code EncryptedDocument}
     * @throws DatabaseConnectionClosedException if the database connection is closed
     */
    private SyncResult syncChange(EncryptedDocument rootEncryptedDocument,
                                  Map<String, RemoteChange> folderChanges,
                                  RemoteChange change)
            throws StorageCryptException, DatabaseConnectionClosedException {
        LOG.debug(" - syncChange() : ");
        if (change.isDeleted()) {
            String deletedDocumentId = change.getDocumentId();
            LOG.debug("   - skipped deleted = {}", change.getDocumentId());
            //TODO: delete the local document if the related property says we should,
            //      and if it is a directory, only if it is empty
            return new SyncResult(SyncResult.Result.Ignored);
        } else {
            LOG.debug("   - created or modified = {}", change.getDocument().getName());
            RemoteDocument remoteDocument = change.getDocument();
            if (remoteDocument.isFolder()) {
                LOG.debug("     - skipped folder = {}", change.getDocument().getName());
                //skip if it is a folder, and wait for .metadata file
                return new SyncResult(SyncResult.Result.Ignored);
            } else {
                String encryptedMetadata;
                if (Constants.STORAGE.FOLDER_METADATA_FILE_NAME.equals(remoteDocument.getName())) {
                    LOG.debug("     - folder metadata = {}", change.getDocument().getName());
                    //if we have a folder .metadata file, create the local folder
                    try {
                        byte[] data = remoteDocument.downloadData();
                        encryptedMetadata = crypto.encodeUrlSafeBase64(data);
                        // get the parent folder document
                        LOG.debug("       - parent id : {}", remoteDocument.getParentId());
                        RemoteChange parentChange = folderChanges.get(remoteDocument.getParentId());
                        if (null!=parentChange && !parentChange.isDeleted() && null!=parentChange.getDocument()) {
                            LOG.debug("       - parent found in changes");
                            remoteDocument = parentChange.getDocument();
                        } else {
                            LOG.error("Parent {} not found in changes for document {}",
                                    remoteDocument.getParentId(), remoteDocument.getName());
                            // try to get it anyway
                            RemoteStorage storage =
                                    rootEncryptedDocument.getBackStorageAccount().getRemoteStorage();
                            if (null == storage) {
                                throw new StorageCryptException("Failed to get parent",
                                        StorageCryptException.Reason.ParentNotFound);
                            }
                            remoteDocument = storage.folder(remoteDocument.getAccountName(), remoteDocument.getParentId());
                            if (null == remoteDocument) {
                                throw new StorageCryptException("Failed to get parent",
                                        StorageCryptException.Reason.ParentNotFound);
                            }
                        }
                    } catch (RemoteException e) {
                        LOG.error("Failed to access remote folder metadata {}", remoteDocument.getName(), e);
                        throw new StorageCryptException("Failed to access remote folder metadata",
                                StorageCryptException.Reason.FailedToGetMetadata, e);
                    }
                } else {
                    encryptedMetadata = remoteDocument.getName();
                }

                EncryptedDocumentMetadata encryptedDocumentMetadata =
                        new EncryptedDocumentMetadata(crypto, keyManager);
                try {
                    encryptedDocumentMetadata.decrypt(encryptedMetadata);

                    LOG.debug("     - decrypted name = {}", encryptedDocumentMetadata.getDisplayName());
                    if (null!= progressListener) {
                        progressListener.onMessage(1, encryptedDocumentMetadata.getDisplayName());
                    }
                } catch (StorageCryptException e) {
                    progressListener.onMessage(1, "");
                    throw e;
                }

                EncryptedDocument parentEncryptedDocument;
                if (null==remoteDocument.getParentId()) {
                    parentEncryptedDocument = null;
                } else if (rootEncryptedDocument.getBackEntryId().equals(remoteDocument.getParentId())) {
                    parentEncryptedDocument = rootEncryptedDocument;
                } else {
                    parentEncryptedDocument = encryptedDocuments.encryptedDocumentWithAccountAndEntryId(
                            rootEncryptedDocument.getBackStorageAccount(), remoteDocument.getParentId());
                }
                if (null== parentEncryptedDocument) {
                    LOG.error("     - parent not found : {}", remoteDocument.getParentId());
                    LOG.error("Parent {} not found for document {}", remoteDocument.getParentId(), remoteDocument.getName());
                    throw new StorageCryptException("Failed to get parent",
                            StorageCryptException.Reason.ParentNotFound);
                } else {
                    EncryptedDocument encryptedDocument = parentEncryptedDocument.child(encryptedDocumentMetadata.getDisplayName());
                    if (null == encryptedDocument) {
                        LOG.debug("     - creating document = {}", encryptedDocumentMetadata.getDisplayName());
                        encryptedDocument = parentEncryptedDocument.createChild(
                                encryptedDocumentMetadata, encryptedMetadata, remoteDocument);
                        return new SyncResult(SyncResult.Result.Synced, encryptedDocument);
                    } else {
                        LOG.debug("     - existing document = {}", encryptedDocumentMetadata.getDisplayName());
                        if (!encryptedDocument.isFolder()
                                && null != encryptedDocument.getBackEntryId()
                                && encryptedDocument.getBackEntryVersion() < remoteDocument.getVersion()) {
                            if (encryptedDocument.getSyncState(SyncAction.Download) == State.Done) {
                                encryptedDocument.updateSyncState(SyncAction.Download, State.Planned);
                                LOG.debug("       - updating document = {}", encryptedDocumentMetadata.getDisplayName());
                                return new SyncResult(SyncResult.Result.Synced, encryptedDocument);
                            } else {
                                LOG.debug("       - ignored document = {}", encryptedDocumentMetadata.getDisplayName());
                                return new SyncResult(SyncResult.Result.Ignored, encryptedDocument);
                            }
                        } else {
                            LOG.debug("       - ignored folder = {}", encryptedDocumentMetadata.getDisplayName());
                            return new SyncResult(SyncResult.Result.Ignored, encryptedDocument);
                        }
                    }
                }
            }
        }
    }
}
