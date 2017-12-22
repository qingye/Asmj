package com.chris.gplugins

import org.apache.tools.ant.BuildEvent
import org.apache.tools.ant.BuildListener
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.tasks.TaskState
import org.gradle.internal.time.Clock

public class CompileCostListener implements TaskExecutionListener, BuildListener {

    private Clock clock;
    private times = [];

    @Override
    void buildStarted(BuildEvent buildEvent) {

    }

    @Override
    void buildFinished(BuildEvent buildEvent) {
        println 'Build finished, total cost:';
        for (t in times) {
            println "%7sms %s\n", t;
        }
    }

    @Override
    void targetStarted(BuildEvent buildEvent) {

    }

    @Override
    void targetFinished(BuildEvent buildEvent) {

    }

    @Override
    void taskStarted(BuildEvent buildEvent) {

    }

    @Override
    void taskFinished(BuildEvent buildEvent) {

    }

    @Override
    void messageLogged(BuildEvent buildEvent) {

    }

    @Override
    void beforeExecute(Task task) {
        clock = new Clock();
    }

    @Override
    void afterExecute(Task task, TaskState taskState) {
        def ms = clock.getElapsedMillis();
        times.add([ms, task.path]);
        task.project.logger.warn "\t=>${task.path} cost ${ms}ms";
    }
}