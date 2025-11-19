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

public class AnxietyFragment extends Fragment {

    private String[] questions = {
            "Feeling nervous, anxious or on edge",
            "Not being able to stop or control worrying",
            "Worrying too much about different things",
            "Trouble relaxing",
            "Being so restless that it is hard to sit still",
            "Becoming easily annoyed or irritable",
            "Feeling afraid as if something awful might happen"
    };

    private RadioGroup[] radioGroups = new RadioGroup[7];

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_anxiety, container, false);

        TextView instructionText = view.findViewById(R.id.instructionText);
        instructionText.setText("Over the last 2 weeks, how often have you been bothered by the following problems?");

        radioGroups[0] = view.findViewById(R.id.radioGroup1);
        radioGroups[1] = view.findViewById(R.id.radioGroup2);
        radioGroups[2] = view.findViewById(R.id.radioGroup3);
        radioGroups[3] = view.findViewById(R.id.radioGroup4);
        radioGroups[4] = view.findViewById(R.id.radioGroup5);
        radioGroups[5] = view.findViewById(R.id.radioGroup6);
        radioGroups[6] = view.findViewById(R.id.radioGroup7);

        TextView[] questionTexts = {
                view.findViewById(R.id.question1),
                view.findViewById(R.id.question2),
                view.findViewById(R.id.question3),
                view.findViewById(R.id.question4),
                view.findViewById(R.id.question5),
                view.findViewById(R.id.question6),
                view.findViewById(R.id.question7)
        };

        for (int i = 0; i < questions.length; i++) {
            questionTexts[i].setText((i + 1) + ". " + questions[i]);
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