package com.jonasgerdes.stoppelmap.widget.options;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.jonasgerdes.stoppelmap.R;
import com.jonasgerdes.stoppelmap.widget.ChangeableFontWidgetPreview;
import com.jonasgerdes.stoppelmap.widget.silhouette.SilhouetteWidgetProvider;

/**
 * Created by jonas on 23.02.2017.
 */

public class TextOptionPage extends OptionPage<ChangeableFontWidgetPreview> {


    public static final String PARAM_DEFAULT_FONT = "PARAM_DEFAULT_FONT";
    public static final String PARAM_SHOW_HOURS = "PARAM_SHOW_HOURS";

    CheckBox mHourToggle;
    RadioGroup mFontSelection;
    RadioButton mFontSelectionRoboto;
    RadioButton mFontSelectionRobotoSlab;
    RadioButton mFontSelectionDamion;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.widget_settings_font_selection_page, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mHourToggle = view.findViewById(R.id.toggle_hours);
        mFontSelection = view.findViewById(R.id.font_selection);
        mFontSelectionRoboto = view.findViewById(R.id.font_roboto);
        mFontSelectionRobotoSlab = view.findViewById(R.id.font_roboto_slab);
        mFontSelectionDamion = view.findViewById(R.id.font_damion);

        mHourToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                getEditableWidgetPreview().setShowHours(isChecked);
                getWidgetPreview().update();
            }
        });

        mHourToggle.setChecked(
                getArguments() != null && getArguments().getBoolean(PARAM_SHOW_HOURS, false)
        );

        String defaultFont = null;
        if (getArguments() != null && getArguments().containsKey(PARAM_DEFAULT_FONT)) {
            defaultFont = getArguments().getString(PARAM_DEFAULT_FONT);
        }

        setUpRadioButton(mFontSelectionRoboto, SilhouetteWidgetProvider.FONT_ROBOTO, defaultFont);
        setUpRadioButton(mFontSelectionRobotoSlab, SilhouetteWidgetProvider.FONT_ROBOTO_SLAB,
                defaultFont);
        setUpRadioButton(mFontSelectionDamion, SilhouetteWidgetProvider.FONT_DAMION, defaultFont);


    }

    private void setUpRadioButton(RadioButton button, final String fontFile, final String
            defaultFont) {
        button.setTypeface(Typeface.createFromAsset(getContext().getAssets(),
                "font/" + fontFile)
        );
        button.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    getEditableWidgetPreview().setFont(fontFile);
                    getWidgetPreview().update();
                }
            }
        });

        if (fontFile.equals(defaultFont)) {
            mFontSelection.check(button.getId());
        }
    }

    public static TextOptionPage newInstance(String defaultFont, boolean showHours) {

        Bundle args = new Bundle();
        args.putString(PARAM_DEFAULT_FONT, defaultFont);
        args.putBoolean(PARAM_SHOW_HOURS, showHours);
        TextOptionPage fragment = new TextOptionPage();
        fragment.setArguments(args);
        return fragment;
    }

}
