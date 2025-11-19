package com.example.project;

// DepressionFragment.java (PHQ-9)

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

public class DepressionFragment extends Fragment {

    private String[] questions = {
            "Little interest or pleasure in doing things",
            "Feeling down, depressed, or hopeless",
            "Trouble falling or staying asleep, or sleeping too much",
            "Feeling tired or having little energy",
            "Poor appetite or overeating",
            "Feeling bad about yourself — or that you are a failure or have let yourself or your family down",
            "Trouble concentrating on things, such as reading the newspaper or watching television",
            "Moving or speaking so slowly that other people could have noticed? Or being so fidgety or restless that you have been moving around a lot more than usual",
            "Thoughts that you would be better off dead or of hurting yourself"
    };

    private RadioGroup[] radioGroups = new RadioGroup[9];

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_depression, container, false);

        // Instruction text
        TextView instructionText = view.findViewById(R.id.instructionText);
        instructionText.setText("Over the last 2 weeks, how often have you been bothered by any of the following problems?");

        // Container IDs from fragment_depression.xml
        int[] containerIds = {
                R.id.question1Container,
                R.id.question2Container,
                R.id.question3Container,
                R.id.question4Container,
                R.id.question5Container,
                R.id.question6Container,
                R.id.question7Container,
                R.id.question8Container,
                R.id.question9Container
        };

        // Loop through and bind question + radio group
        for (int i = 0; i < questions.length; i++) {
            View questionContainer = view.findViewById(containerIds[i]);

            TextView questionText = questionContainer.findViewById(R.id.question);
            questionText.setText((i + 1) + ". " + questions[i]);

            radioGroups[i] = questionContainer.findViewById(R.id.radioGroup);
        }

        return view;
    }

    public int calculateScore() {
        int totalScore = 0;
        boolean allAnswered = true;

        for (RadioGroup rg : radioGroups) {
            int selectedId = rg.getCheckedRadioButtonId();
            if (selectedId == -1) {
                allAnswered = false;
                continue;
            }

            RadioButton selectedButton = rg.findViewById(selectedId);
            String tag = selectedButton.getTag().toString();
            totalScore += Integer.parseInt(tag);
        }

        return allAnswered ? totalScore : -1;
    }
}
