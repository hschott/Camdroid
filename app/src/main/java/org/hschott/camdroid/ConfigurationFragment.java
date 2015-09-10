package org.hschott.camdroid;

import android.app.Activity;
import android.os.Bundle;
import android.app.Fragment;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Toast;

public abstract class ConfigurationFragment extends Fragment {

    private Toast toast;

    public ConfigurationFragment() {
        super();
    }

    public abstract int getLayoutId();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        this.toast = Toast.makeText(this.getActivity(), null,
                Toast.LENGTH_SHORT);

        View c = inflater.inflate(R.layout.configcontainer_ui, null);

        View v = inflater.inflate(this.getLayoutId(), null);
        ((ViewGroup) c.findViewById(R.id.container))
                .addView(v);

        return c;
    }

    @Override
    public void onPause() {
        super.onPause();
    }



    public void showValue(Object text) {
        if (this.toast != null) {
            this.toast.setText(text.toString());
            this.toast.show();
        }
    }

}