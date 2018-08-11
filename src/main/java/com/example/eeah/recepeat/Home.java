package com.example.eeah.recepeat;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link Home#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Home extends Fragment implements View.OnClickListener{
    private CardView card1, card2, card3;
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;


    public Home() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment Home.
     */
    // TODO: Rename and change types and number of parameters
    public static Home newInstance(String param1, String param2) {
        Home fragment = new Home();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        // Inflate the layout for this fragment
        card1 = (CardView) view.findViewById(R.id.card_view1);
        card1.setOnClickListener(this);
        card2 = (CardView) view.findViewById(R.id.card_view2);
        card2.setOnClickListener(this);
        card3 = (CardView) view.findViewById(R.id.card_view3);
        card3.setOnClickListener(this);
        return view;
    }

    @Override
    public void onClick(View view) {
        Fragment fragment = null;
        FragmentManager fragmentManager = getFragmentManager();
        switch(view.getId()){
            case R.id.card_view1:
                fragment = new ReportSummary();
                break;
            case R.id.card_view2:
                fragment = new MealRecommendation();
                break;
            case R.id.card_view3:
                fragment = new LeaderBoard();
                break;
        }
        fragmentManager.beginTransaction()
                .replace(R.id.contentFragment, fragment)
                .addToBackStack(null)
                .commit();
    }
}
