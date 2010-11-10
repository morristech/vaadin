package com.vaadin.tests.components.splitpanel;

import com.vaadin.terminal.Sizeable;
import com.vaadin.tests.components.TestBase;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.SplitPanel.SplitterClickEvent;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalSplitPanel;

public class SplitPanelReversePosition extends TestBase {

    private boolean hsplitReversed = true;
    private boolean vsplitReversed = true;

    @Override
    protected void setup() {
        getLayout().setSizeFull();
        getLayout().setSpacing(true);

        final HorizontalSplitPanel hsplit = new HorizontalSplitPanel();
        hsplit.setSizeFull();
        hsplit.setImmediate(true);
        hsplit.setSplitPosition(100, Sizeable.UNITS_PIXELS, hsplitReversed);
        hsplit.addListener(new HorizontalSplitPanel.SplitterClickListener() {
            public void splitterClick(SplitterClickEvent event) {
                getMainWindow().showNotification("Horizontal Splitter Clicked");
            }
        });

        TextField field = new TextField("");
        field.setSizeFull();
        hsplit.addComponent(field);

        final VerticalSplitPanel vsplit = new VerticalSplitPanel();
        vsplit.setSizeFull();
        vsplit.setImmediate(true);
        vsplit.setSplitPosition(10, Sizeable.UNITS_PERCENTAGE, vsplitReversed);
        vsplit.addListener(new VerticalSplitPanel.SplitterClickListener() {
            public void splitterClick(SplitterClickEvent event) {
                getMainWindow().showNotification("Vertical Splitter Clicked");
            }
        });
        hsplit.addComponent(vsplit);

        addComponent(hsplit);

        field = new TextField("");
        field.setSizeFull();
        vsplit.addComponent(field);

        field = new TextField("");
        field.setSizeFull();
        vsplit.addComponent(field);

        HorizontalLayout buttons = new HorizontalLayout();
        buttons.setSpacing(true);

        buttons.addComponent(new Button("Swap horizontal positioning",
                new Button.ClickListener() {
                    public void buttonClick(ClickEvent event) {
                        hsplitReversed = !hsplitReversed;
                        hsplit.setSplitPosition(100, Sizeable.UNITS_PIXELS,
                                hsplitReversed);

                    }
                }));

        buttons.addComponent(new Button("Swap vertical positioning",
                new Button.ClickListener() {
                    public void buttonClick(ClickEvent event) {
                        vsplitReversed = !vsplitReversed;
                        vsplit.setSplitPosition(10, Sizeable.UNITS_PERCENTAGE,
                                vsplitReversed);
                    }
                }));

        addComponent(buttons);
        
    }

    @Override
    protected String getDescription() {
        return "The horizontal split panel should be splitted "
                + "100px from the right and the vertical split panel should "
                + "be splitted 10% from the bottom";

    }

    @Override
    protected Integer getTicketNumber() {
        return 1588;
    }

}