/*
 * Copyright 2009 JBoss, a divison Red Hat, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.bpm.console.client.process.v2;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;
import com.mvc4g.client.Controller;
import com.mvc4g.client.Event;
import com.mvc4g.client.ViewInterface;
import org.gwt.mosaic.ui.client.*;
import org.gwt.mosaic.ui.client.layout.*;
import org.jboss.bpm.console.client.common.DataDriven;
import org.jboss.bpm.console.client.common.LoadingOverlay;
import org.jboss.bpm.console.client.model.ProcessDefinitionRef;
import org.jboss.bpm.console.client.process.*;
import org.jboss.errai.workspaces.client.api.ProvisioningCallback;
import org.jboss.errai.workspaces.client.api.WidgetProvider;
import org.jboss.errai.workspaces.client.api.annotations.LoadTool;
import org.jboss.errai.workspaces.client.framework.Registry;

import java.util.List;

/**
 * @author: Heiko Braun <hbraun@redhat.com>
 * @date: Oct 15, 2010
 */

@LoadTool(name = "Active Instances", group = "Processes", icon = "processIcon", priority = 1)
public class Explorer implements WidgetProvider, DataDriven, ViewInterface {

    private LayoutPanel layout;
    private LayoutPanel definitionPanel;

    private ToolButton menuButton;
    private HTML title;
    private String selectedGroup;

    private Controller controller;

    private ProcessGroups processGroups = null;

    private ProcessDefinitionRef activeDefinition;

    public void setController(Controller controller) {
        this.controller = controller;
    }

    public void provideWidget(ProvisioningCallback callback)
    {

        initController();

        // -----------------------

        layout = new LayoutPanel(new BorderLayout());

        // -----------------------

        definitionPanel = new LayoutPanel(new BoxLayout(BoxLayout.Orientation.VERTICAL));        
        
        final ToolBar toolBar = new ToolBar();
        definitionPanel.add(toolBar, new BoxLayoutData(BoxLayoutData.FillStyle.HORIZONTAL));

        // -----------------------

        menuButton = new ToolButton("Open");
        menuButton.setStyle(ToolButton.ToolButtonStyle.MENU);
        final Command selectProcessCmd = new Command() {
            public void execute()
            {
                controller.handleEvent(
                        new Event(UpdateDefinitionsAction.ID, null)
                );
            }
        };

        // -----------------------

        PopupMenu menuBtnMenu = new PopupMenu();
        menuBtnMenu.addItem("ProcessDefintion", selectProcessCmd);
        menuButton.setMenu(menuBtnMenu);
        toolBar.add(menuButton);

        title = new HTML();
        title.getElement().setAttribute("style", "font-size:24px; font-weight:BOLD");

        // -----------------------

        LayoutPanel headerPanel = new LayoutPanel(new ColumnLayout());
        headerPanel.add(title, new ColumnLayoutData("70%"));

        LayoutPanel actionPanel = new LayoutPanel(new BoxLayout(BoxLayout.Orientation.VERTICAL));
        actionPanel.getElement().setAttribute("style", "margin-right:10px;");
        ToolButton actions = new ToolButton("Action");
        actions.setStyle(ToolButton.ToolButtonStyle.MENU);
        final Command blank = new Command() {
            public void execute()
            {
            }
        };

        // -----------------------

        PopupMenu actionMenu = new PopupMenu();
        actionMenu.addItem("Properties", blank);
        actionMenu.addItem("Process Diagram", blank);
        actionMenu.addItem("Execution History", blank);
        actions.setMenu(actionMenu);

        actions.getElement().setAttribute("style", "widht:30px; height:12px; padding-right:0px;background-image:none;");
        
        actionPanel.add(actions, new BoxLayoutData(BoxLayoutData.FillStyle.HORIZONTAL));
        headerPanel.add(actionPanel, new ColumnLayoutData("30%"));
        
        definitionPanel.add(headerPanel, new BoxLayoutData(BoxLayoutData.FillStyle.BOTH));

        // -----------------------

        InstanceListView instanceView = new InstanceListView();
        final DecoratedTabLayoutPanel tabPanel = new DecoratedTabLayoutPanel(false);
        instanceView.provideWidget(new ProvisioningCallback()
        {
            public void onSuccess(Widget instance) {
                tabPanel.add(instance, "Active Instances");
            }

            public void onUnavailable() {
            }
        });
       
        layout.add(definitionPanel, new BorderLayoutData(BorderLayout.Region.NORTH, 150));
        layout.add(tabPanel);

        callback.onSuccess(layout);
    }

    private void initController() {
        Controller controller = Registry.get(Controller.class);
        controller.addView(Explorer.class.getName(), this);
        controller.addAction(UpdateInstancesAction.ID, new UpdateInstancesAction());
        controller.addAction(StartNewInstanceAction.ID, new StartNewInstanceAction());
        controller.addAction(StateChangeAction.ID, new StateChangeAction());
        controller.addAction(DeleteDefinitionAction.ID, new DeleteDefinitionAction());
        controller.addAction(DeleteInstanceAction.ID, new DeleteInstanceAction());
        controller.addAction(UpdateDefinitionsAction.ID, new UpdateDefinitionsAction());
    }

    public void reset() {

    }

    public void update(Object... data) {
        this.processGroups = new ProcessGroups((List<ProcessDefinitionRef>)data[0]);
        selectDefinition();
    }

    public void setLoading(boolean isLoading) {
        LoadingOverlay.on(definitionPanel, isLoading);
    }

    private void selectDefinition()
    {
        final LayoutPopupPanel popup = new LayoutPopupPanel(false);
        popup.addStyleName("soa-PopupPanel");

        final ListBox listBox = new ListBox();
        listBox.addItem("");

        assert processGroups!=null : "process definitions not loaded";

        for(String group : processGroups.getGroups())
        {
            listBox.addItem(group);
        }

        // show dialogue
        LayoutPanel p = new LayoutPanel(new BoxLayout(BoxLayout.Orientation.VERTICAL));
        p.add(new HTML("Please select a process:"));
        p.add(listBox);

        // -----

        LayoutPanel p2 = new LayoutPanel(new BoxLayout(BoxLayout.Orientation.HORIZONTAL));
        p2.add(new Button("Done", new ClickHandler() {
            public void onClick(ClickEvent clickEvent)
            {
                if(listBox.getSelectedIndex()>0)
                {
                    popup.hide();
                    selectedGroup = listBox.getItemText(listBox.getSelectedIndex());

                    String name = selectedGroup; // riftsaw name juggling
                    String subtitle = "";
                    if(selectedGroup.indexOf("}")!=-1)
                    {

                        String[] qname = selectedGroup.split("}");
                        name = qname[1];
                        subtitle = qname[0].substring(1, qname[0].length());
                    }

                    String nameAndSubtitle = name + "<br/><div style='color:#C8C8C8;font-size:12px;text-align:left;'>" + subtitle + "</div>";
                    StringBuffer sb = new StringBuffer("<p/><div style='font-size:12px;text-align:left;'>Active Version: ");

                    for(ProcessDefinitionRef groupMemmber : processGroups.getProcessesForGroup(selectedGroup))
                    {
                        if(!groupMemmber.isSuspended())
                        {
                            activeDefinition = groupMemmber;
                            sb.append(groupMemmber.getVersion());
                            break;
                        }
                    }
                    sb.append("</div>");

                    title.setHTML(nameAndSubtitle+sb.toString());

                    DeferredCommand.addCommand(new Command()
                    {
                        public void execute() {
                            controller.handleEvent(
                                    new Event(
                                            UpdateInstancesAction.ID,
                                            getActiveDefinition()
                                    )
                            );
                        }
                    });

                }
            }
        }));

        // -----

        HTML html = new HTML("Cancel");
        html.addClickHandler(new ClickHandler(){
            public void onClick(ClickEvent clickEvent)
            {
                popup.hide();
            }
        });
        p2.add(html, new BoxLayoutData(BoxLayoutData.FillStyle.HORIZONTAL));
        p.add(p2);

        // -----

        popup.setPopupPosition(menuButton.getAbsoluteLeft()-2, menuButton.getAbsoluteTop()+30);
        popup.setWidget(p);
        popup.pack();
        popup.show();
    }

    private ProcessDefinitionRef getActiveDefinition() {
        return activeDefinition;
    }

}
