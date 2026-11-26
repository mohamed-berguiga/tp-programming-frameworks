package net.thevpc.gaming.helloworld;

import net.thevpc.gaming.atom.presentation.Alignment;
import net.thevpc.gaming.atom.presentation.components.SLabel;
import net.thevpc.gaming.atom.presentation.components.SceneComponentState;
import net.thevpc.gaming.atom.presentation.components.TextStyle;

import java.awt.Color;
import java.awt.Font;

final class Styles {
    private Styles() {
    }

    static SLabel style(SLabel label, int fontSize, Color color) {
        label.setTextStyle(SceneComponentState.DEFAULT, new TextStyle(label.getName() + "Style")
                .setFont(new Font("verdana", Font.BOLD, fontSize))
                .setForeColor(color)
                .setAlignment(Alignment.CENTER));
        return label;
    }
}
