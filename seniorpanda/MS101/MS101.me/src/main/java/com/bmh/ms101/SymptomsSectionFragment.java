package com.bmh.ms101;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

/**
 * A dummy fragment representing a section of the app, but that simply displays dummy text.
 */
public class SymptomsSectionFragment extends Fragment {

    public static final String ARG_SECTION_NUMBER = "section_number";

    private Activity mAct;
    private LinearLayout mItems;
    private int mSection;

    public SymptomsSectionFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAct = getActivity();
        // Figure out which layout we're meant to show
        mSection = getArguments().getInt(ARG_SECTION_NUMBER);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mAct.setProgressBarIndeterminateVisibility(false);
        View rootView = inflater.inflate(R.layout.fragment_symptoms, container, false);
        mItems = (LinearLayout) rootView.findViewById(R.id.items);
        setupLabels(inflater);
        setupPainScale(rootView);
        return rootView;
    }

    /**
     * Sets up our ImageView that contains our pain scale so that we can show the tutorial from it
     * @param root The root view in which our image view resides
     */
    private void setupPainScale(View root) {
        ImageView scale = (ImageView) root.findViewById(R.id.pain_scale);
        if (scale != null) {
            scale.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    switch (mSection) {
                        case SymptomsActivity.SECTION_SYMP:
                            Util.buildInfoDialog(mAct, R.string.help_symptom_scales_title,
                                    R.string.help_symptom_scales_content, R.string.okay);
                            break;
                        case SymptomsActivity.SECTION_STRESS:
                            Util.buildInfoDialog(mAct, R.string.help_stress_factor_scales_title,
                                    R.string.help_stress_factor_scales_content, R.string.okay);
                            break;
                    }
                }
            });
        }
    }

    /**
     * Set up the labels on the radio groups based upon the section we are
     * @param inflater A LayoutInflater to use
     */
    private void setupLabels(LayoutInflater inflater) {
        int labelsId = -1; // What will be a resource ID for an array of strings to use as labels
        switch (mSection) {
            case SymptomsActivity.SECTION_SYMP:
                labelsId = R.array.symptoms;
                break;
            case SymptomsActivity.SECTION_STRESS:
                labelsId = R.array.stress_factors;
        }
        String[] labels = getResources().getStringArray(labelsId);
        // For each symptom type, add label and set of radio buttons.
        for (String label : labels) {
            LinearLayout labelAndRBs = (LinearLayout) inflater.inflate(R.layout.symptoms_item, null);
            TextView textView = (TextView) labelAndRBs.findViewById(R.id.symptomLabel);
            textView.setText(label);
            mItems.addView(labelAndRBs, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1));
        }
    }

    /**
     * Passes the items up to the containing activity so that it can report them, tag them with the
     * section number for convenience
     */
    public LinearLayout getItems() {
        return mItems;
    }
}