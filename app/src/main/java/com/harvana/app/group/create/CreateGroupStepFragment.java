package com.harvana.app.group.create;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * One page of the create-group wizard. Each page is its own fragment (Basics, the type's own page, People, Review); the
 * wizard screen holds what has been typed so far and draws the page's fields into it.
 */
public abstract class CreateGroupStepFragment extends Fragment {

    /** The wizard screen: draws one page's fields into {@code page}. */
    public interface Host {
        void buildBasics(LinearLayout page);
        void buildRounds(LinearLayout page);
        void buildSavingsRules(LinearLayout page);
        void buildGoal(LinearLayout page);
        void buildPeople(LinearLayout page);
        void buildReview(LinearLayout page);
        void pageShown(CreateGroupStepFragment f);
    }

    private LinearLayout page;
    private ScrollView scroll;

    /** Fills this page's fields (its own layout, already in {@code page}). */
    protected abstract void build(Host host, LinearLayout page);

    /** This page's layout: res/layout/fragment_create_*.xml. */
    protected abstract int layout();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        View v = inflater.inflate(com.harvana.app.R.layout.fragment_create_step, container, false);
        scroll = v.findViewById(com.harvana.app.R.id.create_step_scroll);
        page = v.findViewById(com.harvana.app.R.id.create_step_page);
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        redraw();
        if (getActivity() instanceof Host) ((Host) getActivity()).pageShown(this);
    }

    /** Draws the page again from what the wizard holds (after a choice changed something on it). */
    public void redraw() {
        if (page == null || !(getActivity() instanceof Host)) return;
        page.removeAllViews();
        getLayoutInflater().inflate(layout(), page, true);
        build((Host) getActivity(), page);
        com.harvana.app.utils.IconFit.apply(page);
    }

    public void toTop() { if (scroll != null) scroll.post(() -> scroll.scrollTo(0, 0)); }

    @Override
    public void onDestroyView() {
        page = null;
        scroll = null;
        super.onDestroyView();
    }
}
