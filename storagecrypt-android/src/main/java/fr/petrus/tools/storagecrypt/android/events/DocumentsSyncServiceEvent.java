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

package fr.petrus.tools.storagecrypt.android.events;

import org.greenrobot.eventbus.EventBus;

import fr.petrus.lib.core.Progress;
import fr.petrus.lib.core.SyncAction;
import fr.petrus.tools.storagecrypt.android.tasks.DocumentsSyncTask;

/**
 * The {@link EventBus} event cast when the {@link DocumentsSyncTask} processing state changes.
 *
 * @author Pierre Sagne
 * @since 15.04.2015
 */
public class DocumentsSyncServiceEvent extends Event {
    private SyncAction syncAction;
    private Progress documentsListProgress;
    private Progress currentDocumentProgress;
    private String currentDocumentName;

    /**
     * Creates a new {@code DocumentsSyncServiceEvent} instance.
     */
    public DocumentsSyncServiceEvent() {
        syncAction = null;
        documentsListProgress = new Progress(0, 0);
        currentDocumentProgress = new Progress(0, 0);
        currentDocumentName = null;
    }

    /**
     * Sets the synchronization action currently running.
     *
     * @param syncAction the synchronization action currently running
     */
    public void setSyncAction(SyncAction syncAction) {
        this.syncAction = syncAction;
    }

    /**
     * Sets the name of the currently processed document.
     *
     * @param currentDocumentName the name of the currently processed document
     */
    public void setCurrentDocumentName(String currentDocumentName) {
        this.currentDocumentName = currentDocumentName;
    }

    /**
     * Returns whether an action us currently running
     *
     * @return true if an action us currently running
     */
    public synchronized boolean isRunning() {
        return null != syncAction;
    }

    /**
     * Returns the synchronization action currently running.
     *
     * @return the synchronization action currently running
     */
    public SyncAction getSyncAction() {
        return syncAction;
    }

    /**
     * Returns the {@code Progress} in the documents list.
     *
     * @return the {@code Progress} in the documents list
     */
    public Progress getDocumentsListProgress() {
        return documentsListProgress;
    }

    /**
     * Returns the {@code Progress} inside the currently processed document.
     *
     * @return the {@code Progress} inside the currently processed document
     */
    public Progress getCurrentDocumentProgress() {
        return currentDocumentProgress;
    }

    /**
     * Returns the name of the currently processed document.
     *
     * @return the name of the currently processed document
     */
    public String getCurrentDocumentName() {
        return currentDocumentName;
    }
}