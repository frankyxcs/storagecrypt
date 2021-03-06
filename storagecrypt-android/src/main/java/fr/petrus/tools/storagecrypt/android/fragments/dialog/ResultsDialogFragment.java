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

package fr.petrus.tools.storagecrypt.android.fragments.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TableRow;
import android.widget.TextView;

import fr.petrus.lib.core.processes.results.BaseProcessResults;
import fr.petrus.tools.storagecrypt.R;
import fr.petrus.tools.storagecrypt.android.events.ShowDialogEvent;

/**
 * This dialog displays the number of successful, failed and skipped results of a task.
 *
 * <p>When the uses clicks one type of results, the detailed list of this type of result is displayed.
 *
 * @author Pierre Sagne
 * @since 11.04.2015
 */
public class ResultsDialogFragment extends CustomDialogFragment<ResultsDialogFragment.Parameters> {
    /**
     * The constant TAG used for logging and the fragment manager.
     */
    private static final String TAG = "ResultsDialogFragment";

    /**
     * The class which holds the parameters to create this dialog.
     */
    public static class Parameters extends CustomDialogFragment.Parameters {
        private BaseProcessResults results = null;
        private String title = null;
        private String message = null;

        /**
         * Creates a new empty {@code Parameters} instance.
         */
        public Parameters() {}

        /**
         * Sets the results to display in the dialog.
         *
         * @param results the results to display in the dialog
         * @return this {@code Parameters} for further configuration
         */
        public Parameters setResults(BaseProcessResults results) {
            this.results = results;
            return this;
        }

        /**
         * Sets the title of the dialog to create.
         *
         * @param title the title of the dialog to create
         * @return this {@code Parameters} for further configuration
         */
        public Parameters setTitle(String title) {
            this.title = title;
            return this;
        }

        /**
         * Sets the message to display in the dialog.
         *
         * @param message the message to display in the dialog
         * @return this {@code Parameters} for further configuration
         */
        public Parameters setMessage(String message) {
            this.message = message;
            return this;
        }

        /**
         * Returns the results to display in the dialog.
         *
         * @return the results to display in the dialog
         */
        public BaseProcessResults getResults() {
            return results;
        }

        /**
         * Returns the title of the dialog to create.
         *
         * @return the title of the dialog to create
         */
        public String getTitle() {
            return title;
        }

        /**
         * Returns the message to display in the dialog.
         *
         * @return the message to display in the dialog
         */
        public String getMessage() {
            return message;
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        View view = layoutInflater.inflate(R.layout.fragment_results, null);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());

        if (null!=parameters) {
            if (null != parameters.getResults()) {
                TextView resultsMessage = (TextView) view.findViewById(R.id.results_message);
                TableRow successRow = (TableRow) view.findViewById(R.id.success_row);
                TableRow skippedRow = (TableRow) view.findViewById(R.id.skipped_row);
                TableRow errorsRow = (TableRow) view.findViewById(R.id.errors_row);
                Button successButton = (Button) view.findViewById(R.id.success_button);
                Button skippedButton = (Button) view.findViewById(R.id.skipped_button);
                Button errorsButton = (Button) view.findViewById(R.id.errors_button);

                if (null!=parameters.getTitle()) {
                    dialogBuilder.setTitle(parameters.getTitle());
                }

                if (null!=parameters.getMessage()) {
                    resultsMessage.setText(parameters.getMessage());
                }

                if (null != parameters.getResults().getSuccessResultsList()) {
                    successButton.setText(String.valueOf(parameters.getResults().getSuccessResultsList().size()));
                    successButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            new ShowDialogEvent(new ResultsListDialogFragment.Parameters()
                                    .setTitle(parameters.getTitle())
                                    .setResults(parameters.getResults())
                                    .setResultsType(BaseProcessResults.ResultsType.Success)).postSticky();
                        }
                    });
                    successRow.setVisibility(View.VISIBLE);
                } else {
                    successRow.setVisibility(View.GONE);
                }

                if (null != parameters.getResults().getSkippedResultsList()) {
                    skippedButton.setText(String.valueOf(parameters.getResults().getSkippedResultsList().size()));
                    skippedButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            new ShowDialogEvent(new ResultsListDialogFragment.Parameters()
                                    .setTitle(parameters.getTitle())
                                    .setResults(parameters.getResults())
                                    .setResultsType(BaseProcessResults.ResultsType.Skipped)).postSticky();
                        }
                    });
                    skippedRow.setVisibility(View.VISIBLE);
                } else {
                    skippedRow.setVisibility(View.GONE);
                }

                if (null != parameters.getResults().getErrorResultsList()) {
                    errorsButton.setText(String.valueOf(parameters.getResults().getErrorResultsList().size()));
                    errorsButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            new ShowDialogEvent(new ResultsListDialogFragment.Parameters()
                                    .setTitle(parameters.getTitle())
                                    .setResults(parameters.getResults())
                                    .setResultsType(BaseProcessResults.ResultsType.Errors)).postSticky();
                        }
                    });
                    errorsRow.setVisibility(View.VISIBLE);
                } else {
                    errorsRow.setVisibility(View.GONE);
                }
            }
        }
        dialogBuilder.setNeutralButton(getActivity().getString(R.string.results_dialog_back_button_text), null);
        dialogBuilder.setView(view);
        AlertDialog dialog = dialogBuilder.create();
        return dialog;
    }

    /**
     * Creates a {@code ResultsDialogFragment} and displays it.
     *
     * @param fragmentManager the fragment manager to add the {@code ResultsDialogFragment} to
     * @param parameters      the parameters to create the {@code ResultsDialogFragment}
     * @return the newly created {@code ResultsDialogFragment}
     */
    public static ResultsDialogFragment showFragment(FragmentManager fragmentManager, Parameters parameters) {
        ResultsDialogFragment fragment = new ResultsDialogFragment();
        fragment.setParameters(parameters);
        fragment.show(fragmentManager, TAG);
        return fragment;
    }
}