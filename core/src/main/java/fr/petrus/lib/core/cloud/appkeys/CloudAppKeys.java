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

package fr.petrus.lib.core.cloud.appkeys;

import fr.petrus.lib.core.cloud.RemoteStorage;

/**
 * This interface is used by {@link RemoteStorage} implementations
 * to retrieve the OAuth2 credentials used to call the Cloud Provider
 * (ie Google Drive or Dropbox) API.
 *
 * @author Pierre Sagne
 * @since 24.03.2016
 */
public interface CloudAppKeys {
    /**
     * Returns true if the API keys were found and successfully loaded.
     *
     * @return true if the API keys were found and successfully loaded
     */
    boolean found();

    /**
     * Returns the OAuth2 app keys for the Google Drive API.
     *
     * @return the OAuth2 app keys for the Google Drive API
     */
    AppKeys getGoogleDriveAppKeys();

    /**
     * Returns the OAuth2 app keys for the Dropbox API.
     *
     * @return the OAuth2 app keys for the Dropbox API
     */
    AppKeys getDropboxAppKeys();

    /**
     * Returns the OAuth2 app keys for the Box API.
     *
     * @return the OAuth2 app keys for the Box API
     */
    AppKeys getBoxAppKeys();

    /**
     * Returns the OAuth2 app keys for the HubiC API.
     *
     * @return the OAuth2 app keys for the HubiC API
     */
    AppKeys getHubicAppKeys();

    /**
     * Returns the OAuth2 app keys for the OneDrive API.
     *
     * @return the OAuth2 app keys for the OneDrive API
     */
    AppKeys getOneDriveAppKeys();
}
