package com.trailbook.kole.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.Toast;

/**
 * Created by kole on 7/20/2014.
 */
public class AlertDialogFragment extends DialogFragment {

    public interface AlertDialogListener {
        public void onCancelClicked(int id);
        public void onOkClicked(int id);
    }

    public static AlertDialogFragment newInstance(String title, String message, String cancel, String ok, int id) {
        AlertDialogFragment frag = new AlertDialogFragment();
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("message", message);
        args.putString("cancel", cancel);
        args.putString("ok", ok);
        args.putInt("id", id);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String title = getArguments().getString("title");
        String message = getArguments().getString("message");
        String cancel = getArguments().getString("cancel");
        String ok = getArguments().getString("ok");
        final int id = getArguments().getInt("id");

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        return builder.setTitle(title)
                .setMessage(message)
                .setNegativeButton(cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        ((AlertDialogListener)getActivity()).onCancelClicked(id);
                    }
                })
                .setPositiveButton(ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        ((AlertDialogListener)getActivity()).onOkClicked(id);
                    }
                })
                .create();
    }
}