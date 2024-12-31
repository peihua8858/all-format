package com.damoguyansi.all.format;

import com.damoguyansi.all.format.ui.NewDialog;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.UIUtil;

public class FormatAction extends AnAction {
    public FormatAction() {
        super("AllFormat");
    }

    public void actionPerformed(AnActionEvent e) {
      Project project= getEventProject(e);
        NewDialog dialog = new NewDialog(project,UIUtil.isUnderDarcula());
    }
}
