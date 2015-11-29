package io.imdone.plugin;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.FileEditorStateLevel;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.ex.VirtualFileManagerEx;
import io.socket.emitter.Emitter;
import io.socket.engineio.client.Socket;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

/**
 * @author <a href="http://about.me/jesse.piascik">Jesse Piascik</a>
 */
public class OpenFileComponent implements ApplicationComponent {
    private static final int PORT = 9799;
    private Socket socket;

    public OpenFileComponent() {
    }

    public void initComponent() {
        // TODO:10 insert component initialization logic here
        // TODO:20 [Quick Start Guide](http://www.jetbrains.org/intellij/sdk/docs/basics.html)
        // TODO:30 [Running and Debugging a Plugin](http://www.jetbrains.org/intellij/sdk/docs/basics/getting_started/running_and_debugging_a_plugin.html)
        try {
            socket = new Socket("ws://localhost:" + PORT);
            socket.on(Socket.EVENT_OPEN, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    socket.send("{\"hello\":\"intellij\"}");
                }
            })
                  .on(Socket.EVENT_MESSAGE, new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            try {
                                JSONObject msg = new JSONObject((String)args[0]);
                                onMessage(msg);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }
                    })
                    .on(Socket.EVENT_ERROR, new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            Exception err = (Exception) args[0];
                        }
                    })
                    .on(Socket.EVENT_CLOSE, new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            try {
                                TimeUnit.MILLISECONDS.sleep(1500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } finally {
                                socket.open();
                            }
                        }
                    });
                    socket.open();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void disposeComponent() {
        // TODO:0 insert component disposal logic here
        socket.close();
    }

    @NotNull
    public String getComponentName() {
        return "OpenFileComponent";
    }

    private Project[] getProjects() {
        return ProjectManagerEx.getInstanceEx().getOpenProjects();
    }

    private void openFile(String basePath, String path, Integer line) {
        for (Project project : getProjects()) {
            String projectPath = project.getBasePath();
            if (projectPath.equals(basePath)) {
                VirtualFile file = LocalFileSystem.getInstance().findFileByPath(path);
                FileEditor[] editors = FileEditorManagerEx.getInstanceEx(project).openFile(file, true);
                for (FileEditor editor : editors) {
                    // DOING: Make sure we're working with a TextEditor
                    LogicalPosition pos = new LogicalPosition(line, 0);
                    ((TextEditor)editor).getEditor().getCaretModel().moveToLogicalPosition(pos);
                    ((TextEditor)editor).getEditor().getScrollingModel().scrollToCaret(ScrollType.CENTER);
                }
            }
        }
    }

    private void onMessage(final JSONObject msg) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    String project = msg.getString("project");
                    String path = msg.getString("path");
                    Integer line = msg.getInt("line");
                    if (project != null && path != null) {
                        openFile(project, path, line);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

    }

}
