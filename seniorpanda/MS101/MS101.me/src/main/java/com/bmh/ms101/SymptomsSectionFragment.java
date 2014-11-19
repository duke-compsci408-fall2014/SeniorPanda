package com.bmh.ms101;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import java.util.List;
import java.util.ArrayList;
/**
 * A dummy fragment representing a section of the app, but that simply displays dummy text.
 */
public class SymptomsSectionFragment extends Fragment {

    public static final String ARG_SECTION_NUMBER = "section_number";

    private Activity mAct;
    private LinearLayout mItems;
    private int mSection;
    private View rootView;

    private List<String> checkedBodyLocations = new ArrayList<String>();

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
     //   View rootView = inflater.inflate(R.layout.fragment_symptoms, container, false);
        rootView = inflater.inflate(R.layout.fragment_symptoms, container, false);
        mItems = (LinearLayout) rootView.findViewById(R.id.items);
        setupLabels(inflater);
        setupPainScale(rootView);
        setupRadioGroup(mItems);
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

    public void setupRadioGroup(LinearLayout symptomsContainer) {
        StringBuilder encoded = new StringBuilder("");

        final CharSequence[] tremors_check={"foot","right hand","left hand"};
        final CharSequence[] slow_move_check={"slow1","slow2","slow3"};
        final CharSequence[] rigidity_check={"Green1","Black2","White3"};
        final CharSequence[] freezing_check={"Green","Black","White"};
        //The array that holds the checked state of the checkbox items
        final boolean checked_state[]={false,false,false,false,false,false};



        // Loops through each RadioGroup to find which RadioButton is checked
        for (int i = 0; i < symptomsContainer.getChildCount(); i++) {
            LinearLayout labelAndSlider = (LinearLayout) symptomsContainer.getChildAt(i);
            // Get the radio group
            RadioGroup radGrp = (RadioGroup) labelAndSlider.findViewById(R.id.symptomRatings);
            int checkedRadioButtonID = radGrp.getCheckedRadioButtonId();
            final int itemSelected = i;
            final CharSequence[] sympCheck = getsymptomsCheck(i);
            final String title = getTitle(i);
            radGrp.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                public void onCheckedChanged(RadioGroup arg0, int id) {
                    switch (id) {
                        case R.id.radio0:
                            Log.v("Debug##", "Choice 0 !");
                           /* Toast.makeText(rootView.getContext(),
                                    "No symptoms",
                                    Toast.).show();*/
                            break;
                        case R.id.radio1:
                        case R.id.radio2:
                        case R.id.radio3:
                        case R.id.radio4:
                        case R.id.radio5:
                            Log.v("Debug##", "Selected radio");

                            /*new AlertDialog.Builder(rootView.getContext()).setMessage("hello")
                                    .setNeutralButton(R.string.okay, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {}
                                    })
                                    .show();*/
/*                            new AlertDialog.Builder(rootView.getContext()).setMessage("hello")
                                    .setNeutralButton(R.string.okay, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {}
                                    })
                                    .show();*/
                            new AlertDialog.Builder(rootView.getContext())
                                    .setTitle(title)
                                    .setMultiChoiceItems(sympCheck, null, new DialogInterface.OnMultiChoiceClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                        // TODO Auto-generated method stub
                                    //storing the checked state of the items in an array
                                    checked_state[which]=isChecked;
                                    }
                                })
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // TODO Auto-generated method stub
                                    String display_checked_symptoms= "";
                                    for(int i=0;i<6;i++){
                                    	if(checked_state[i]==true){
                                            display_checked_symptoms=display_checked_symptoms+":"+sympCheck[i];
                                          }
                                        }
                                    Toast.makeText(rootView.getContext(),
                                            "The selected location(s) is"+display_checked_symptoms,
                                            Toast.LENGTH_LONG).show();
                                    checkedBodyLocations.add(itemSelected, display_checked_symptoms);
                                    //clears the String used to store the displayed text
                                	display_checked_symptoms=null;

                                 //clears the array used to store checked state
                                   for(int i=0;i<checked_state.length;i++){
                                      if(checked_state[i]==true){
                                           checked_state[i]=false;
                                       }
                                     }

                                 //used to dismiss the dialog upon user selection.
                                   dialog.dismiss();
                                  }
                             }).show();
                            break;
                        default:
                            Log.v("Debug##", "Huh?");
                            break;
                    }
                }
            });
           /* encoded.append(i + 1).append(":");
            // Get the checked RadioButton
            RadioButton checkedRadio = (RadioButton) labelAndSlider.findViewById(value
                    .getCheckedRadioButtonId());
            // Get the severity number by getting the checked RadioButton's label
            int severity = Integer.parseInt((String) checkedRadio.getText());
            encoded.append(severity).append(",");
            // Each time this loops, it adds on a piece of string in this format:
            // "[SymptomID]:[SeverityLevel]" where SymptomID is just whether the symptom is the 1st
            // in the list, 2nd in the list, etc.*/
        }
    }

    public String getTitle(int i) {
        String title = "";
        switch (i) {
            case 0:
                title = "Choose a tremor location";
                break;
            case 1:
                title = "Choose a slow movement location";
                break;
            case 2:
                title = "Choose a rigidity location";
                break;
            case 3:
                title = "Choose a freeze location";
                break;
            default:
                break;

        }
        return title;
    }

    public CharSequence[] getsymptomsCheck(int i) {
        CharSequence[] sympCheck = new CharSequence[6];
        switch (i) {
            case 0:
                sympCheck[0] = "Left arm or hand";
                sympCheck[1] = "Right arm or hand";
                sympCheck[2] = "Left leg or foot";
                sympCheck[3] = "Right leg or foot";
                sympCheck[4] = "Body";
                sympCheck[5] = "Head";
                break;
            case 1:
                sympCheck[0] = "Left arm or hand";
                sympCheck[1] = "Right arm or hand";
                sympCheck[2] = "Left leg or foot";
                sympCheck[3] = "Right leg or foot";
                sympCheck[4] = "Body";
                sympCheck[5] = "Head";
                break;
            case 2:
                sympCheck[0] = "Left arm or hand";
                sympCheck[1] = "Right arm or hand";
                sympCheck[2] = "Left leg or foot";
                sympCheck[3] = "Right leg or foot";
                sympCheck[4] = "Body";
                sympCheck[5] = "Head";
                break;
            case 3:
                sympCheck[0] = "Left arm or hand";
                sympCheck[1] = "Right arm or hand";
                sympCheck[2] = "Left leg or foot";
                sympCheck[3] = "Right leg or foot";
                sympCheck[4] = "Body";
                sympCheck[5] = "Head";
                break;
            default:
                break;

        }
        return sympCheck;
    }

    public List<String> getCheckedBodyLocations() {
        return checkedBodyLocations;
    }

    public String getCheckedBodyLocations(int i) {
        return checkedBodyLocations.get(i);
    }

    public void clearCheckedBodyLocations() {
        checkedBodyLocations.clear();
    }

}