/*
 * Copyright (C) 2012 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sufficientlysecure.keychain.ui.dialog;

import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.helper.PgpHelper;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;

public class DeleteKeyDialogFragment extends DialogFragment {
    private static final String ARG_MESSENGER = "messenger";
    private static final String ARG_DELETE_KEY_RING_ROW_ID = "delete_file";
    private static final String ARG_KEY_TYPE = "key_type";

    public static final int MESSAGE_OKAY = 1;

    private Messenger mMessenger;

    /**
     * Creates new instance of this delete file dialog fragment
     */
    public static DeleteKeyDialogFragment newInstance(Messenger messenger, long deleteKeyRingRowId,
            int keyType) {
        DeleteKeyDialogFragment frag = new DeleteKeyDialogFragment();
        Bundle args = new Bundle();

        args.putParcelable(ARG_MESSENGER, messenger);
        args.putLong(ARG_DELETE_KEY_RING_ROW_ID, deleteKeyRingRowId);
        args.putInt(ARG_KEY_TYPE, keyType);

        frag.setArguments(args);

        return frag;
    }

    /**
     * Creates dialog
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final FragmentActivity activity = getActivity();
        mMessenger = getArguments().getParcelable(ARG_MESSENGER);

        final long deleteKeyRingRowId = getArguments().getLong(ARG_DELETE_KEY_RING_ROW_ID);
        final int keyType = getArguments().getInt(ARG_KEY_TYPE);

        // TODO: better way to do this?
        String userId = activity.getString(R.string.unknownUserId);

        if (keyType == Id.type.public_key) {
            PGPPublicKeyRing keyRing = ProviderHelper.getPGPPublicKeyRingByRowId(activity,
                    deleteKeyRingRowId);
            userId = PgpHelper.getMainUserIdSafe(activity, PgpHelper.getMasterKey(keyRing));
        } else {
            PGPSecretKeyRing keyRing = ProviderHelper.getPGPSecretKeyRingByRowId(activity,
                    deleteKeyRingRowId);
            userId = PgpHelper.getMainUserIdSafe(activity, PgpHelper.getMasterKey(keyRing));
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.warning);
        builder.setMessage(getString(
                keyType == Id.type.public_key ? R.string.keyDeletionConfirmation
                        : R.string.secretKeyDeletionConfirmation, userId));
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setPositiveButton(R.string.btn_delete, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int id) {
                if (keyType == Id.type.public_key) {
                    ProviderHelper.deletePublicKeyRing(activity, deleteKeyRingRowId);
                } else {
                    ProviderHelper.deleteSecretKeyRing(activity, deleteKeyRingRowId);
                }

                dismiss();

                sendMessageToHandler(MESSAGE_OKAY);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int id) {
                dismiss();
            }
        });
        return builder.create();
    }

    /**
     * Send message back to handler which is initialized in a activity
     * 
     * @param what
     *            Message integer you want to send
     */
    private void sendMessageToHandler(Integer what) {
        Message msg = Message.obtain();
        msg.what = what;

        try {
            mMessenger.send(msg);
        } catch (RemoteException e) {
            Log.w(Constants.TAG, "Exception sending message, Is handler present?", e);
        } catch (NullPointerException e) {
            Log.w(Constants.TAG, "Messenger is null!", e);
        }
    }
}