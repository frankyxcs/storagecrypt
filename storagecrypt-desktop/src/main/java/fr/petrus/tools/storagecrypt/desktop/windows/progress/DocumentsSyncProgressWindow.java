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

package fr.petrus.tools.storagecrypt.desktop.windows.progress;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import fr.petrus.lib.core.Progress;
import fr.petrus.lib.core.SyncAction;
import fr.petrus.tools.storagecrypt.desktop.tasks.DocumentsSyncTask;
import fr.petrus.tools.storagecrypt.desktop.windows.AppWindow;

import static fr.petrus.tools.storagecrypt.desktop.swt.GridLayoutUtil.applyGridLayout;
import static fr.petrus.tools.storagecrypt.desktop.swt.GridDataUtil.applyGridData;

/**
 * The {@code ProgressWindow} subclass which displays the progress of a {@code DocumentsSyncTask}
 *
 * @see DocumentsSyncTask
 *
 * @author Pierre Sagne
 * @since 28.07.2015
 */
public class DocumentsSyncProgressWindow
        extends ProgressWindow<DocumentsSyncProgressWindow.ProgressEvent, DocumentsSyncTask> {

    /**
     * The {@code ProgressWindow.ProgressEvent} subclass for this progress window
     */
    public static class ProgressEvent extends ProgressWindow.ProgressEvent {

        /**
         * The synchronization action on the currently processed document.
         */
        public SyncAction syncAction = null;

        /**
         * The name of the currently processed document.
         */
        public String documentName = null;

        /**
         * Creates a new {@code ProgressEvent} instance.
         */
        public ProgressEvent() {
            super(new Progress(false), new Progress(false));
        }
    }

    private Label titleLabel;
    private Label documentNameLabel;
    private ProgressEvent syncProgressEvent = new ProgressEvent();

    /**
     * Creates a new {@code DocumentsSyncProgressWindow} instance.
     *
     * @param appWindow the application window
     */
    public DocumentsSyncProgressWindow(AppWindow appWindow) {
        super(appWindow, DocumentsSyncTask.class,
                appWindow.getTextBundle().getString("progress_title_syncing_documents"),
                new ProgressEvent(), new Progress(false), new Progress(false));
        setShellStyle(getShellStyle() | SWT.CLOSE);
    }

    @Override
    protected void createProgressContents(Composite parent) {
        applyGridLayout(parent);

        titleLabel = new Label(parent, SWT.NULL);
        titleLabel.setText(textBundle.getString("progress_message_syncing_documents"));
        applyGridData(titleLabel).withHorizontalFill();

        documentNameLabel = new Label(parent, SWT.NULL);
        applyGridData(documentNameLabel).withHorizontalFill();
    }

    @Override
    protected void updateProgress(ProgressEvent progressEvent) {
        if (null!=progressEvent.syncAction) {
            switch (progressEvent.syncAction) {
                case Download:
                    titleLabel.setText(textBundle.getString("progress_message_syncing_documents_download"));
                    break;
                case Upload:
                    titleLabel.setText(textBundle.getString("progress_message_syncing_documents_upload"));
                    break;
                case Deletion:
                    titleLabel.setText(textBundle.getString("progress_message_syncing_documents_deletion"));
                    break;
            }
        }
        if (null!=progressEvent.documentName) {
            documentNameLabel.setText(progressEvent.documentName);
        }
    }

    /**
     * Update the state of this progress window with the given {@code syncState}.
     *
     * @param syncState the synchronization state to update this window with
     */
    public void update(DocumentsSyncTask.SyncServiceState syncState) {
        syncProgressEvent.syncAction = syncState.currentSyncAction;
        syncProgressEvent.documentName = syncState.currentDocumentName;
        syncProgressEvent.progresses[0].set(syncState.documentsListProgress);
        syncProgressEvent.progresses[1].set(syncState.currentDocumentProgress);
        update(syncProgressEvent);
    }
}