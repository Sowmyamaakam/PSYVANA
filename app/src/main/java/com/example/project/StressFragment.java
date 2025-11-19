package com.example.project;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class StressFragment extends Fragment {

    private String[] questions = {
            "In the last month, how often have you been upset because of something that happened unexpectedly?",
            "In the last month, how often have you felt that you were unable to control the important things in your life?",
            "In the last month, how often have you felt nervous and \"stressed\"?",
            "In the last month, how often have you felt confident about your ability to handle your personal problems? (Reverse)",
            "In the last month, how often have you felt that things were going your way? (Reverse)",
            "In the last month, how often have you found that you could not cope with all the things you had to do?",
            "In the last month, how often have you been able to control irritations in your life? (Reverse)",
            "In the last month, how often have you felt that you were on top of things? (Reverse)",
            "In the last month, how often have you been angered because of things that were outside of your control?",
            "In the last month, how often have you felt difficulties were piling up so high that you could not overcome them?"
    };

    // Items 4, 5, 7, 8 are reverse scored
    private boolean[] reverseScored = {false, false, false, true, true, false, true, true, false, false};

    private RadioGroup[] radioGroups = new RadioGroup[10];

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stress, container, false);

        TextView instructionText = view.findViewById(R.id.instructionText);
        instructionText.setText("In the last month, how often have you felt or thought the following?");

        radioGroups[0] = view.findViewById(R.id.radioGroup1);
        radioGroups[1] = view.findViewById(R.id.radioGroup2);
        radioGroups[2] = view.findViewById(R.id.radioGroup3);
        radioGroups[3] = view.findViewById(R.id.radioGroup4);
        radioGroups[4] = view.findViewById(R.id.radioGroup5);
        radioGroups[5] = view.findViewById(R.id.radioGroup6);
        radioGroups[6] = view.findViewById(R.id.radioGroup7);
        radioGroups[7] = view.findViewById(R.id.radioGroup8);
        radioGroups[8] = view.findViewById(R.id.radioGroup9);
        radioGroups[9] = view.findViewById(R.id.radioGroup10);

        TextView[] questionTexts = {
                view.findViewById(R.id.question1),
                view.findViewById(R.id.question2),
                view.findViewById(R.id.question3),
                view.findViewById(R.id.question4),
                view.findViewById(R.id.question5),
                view.findViewById(R.id.question6),
                view.findViewById(R.id.question7),
                view.findViewById(R.id.question8),
                view.findViewById(R.id.question9),
                view.findViewById(R.id.question10)
        };

        for (int i = 0; i < questions.length; i++) {
            questionTexts[i].setText((i + 1) + ". " + questions[i]);
        }

        return view;
    }

    public int calculateScore() {
        int totalScore = 0;
        boolean allAnswered = true;

        for (int i = 0; i < radioGroups.length; i++) {
            RadioGroup rg = radioGroups[i];
            int selectedId = rg.getCheckedRadioButtonId();

            if (selectedId == -1) {
                allAnswered = false;
                continue;
            }

            RadioButton selectedButton = rg.findViewById(selectedId);
            int score = Integer.parseInt(selectedButton.getTag().toString());

            // Apply reverse scoring for items 4, 5, 7, 8
            if (reverseScored[i]) {
                score = 4 - score;  // 0→4, 1→3, 2→2, 3→1, 4→0
            }

            totalScore += score;
        }

        return allAnswered ? totalScore : -1;
    }
}