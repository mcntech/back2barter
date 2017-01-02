package com.mcntech.back2barter;

import android.app.Activity;
import android.support.design.widget.CollapsingToolbarLayout;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;


/**
 * A fragment representing a single Transaction detail screen.
 * This fragment is either contained in a {@link TransactionListActivity}
 * in two-pane mode (on tablets) or a {@link TransactionDetailActivity}
 * on handsets.
 */
public class TransactionDetailFragment extends Fragment {

    private TransactionEx mItem;

    public TransactionDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

            mItem = getArguments().getParcelable("transaction");

            Activity activity = this.getActivity();
            CollapsingToolbarLayout appBarLayout = (CollapsingToolbarLayout) activity.findViewById(R.id.toolbar_layout);
            if (appBarLayout != null) {
                appBarLayout.setTitle(mItem.getName());
            }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.transaction_detail, container, false);

        if (mItem != null) {
            ((TextView) rootView.findViewById(R.id.lbl_transaction_id)).setText("Transaction Id");//setText(mItem.getTxid());
            ((TextView) rootView.findViewById(R.id.txt_transaction_id)).setText(mItem.getTxid());
        }

        return rootView;
    }
}
