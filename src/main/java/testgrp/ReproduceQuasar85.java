package testgrp;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import co.paralleluniverse.fibers.FiberAsync;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.SuspendableRunnable;

import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.concurrent.Phaser;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class ReproduceQuasar85 {

    static final String EngineName = "nashorn";

    // executor for simulation of async operations
    static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static class Env {

        // script will call this method
        @Suspendable
        public String capture() throws Exception {
            return new FiberAsync<String, Exception>() {

                @Override
                protected void requestAsync() {
                    // this should actually be a real async operation, but it's okay for demonstration
                    executor.submit(() -> asyncCompleted("completed"));
                }
            }.run();
        }
    }

    public static void main(String[] args)
            throws InterruptedException, SuspendExecution, ExecutionException {
        final ScriptEngine Engine = new ScriptEngineManager(SuspendExecution.class.getClassLoader()).getEngineByName(EngineName);
        System.out.println(new Fiber() {
            @Override
            protected Object run() throws SuspendExecution, InterruptedException {
                // plug in an instance of Env
                Engine.getContext().setAttribute("env", new Env(), ScriptContext.ENGINE_SCOPE);
                try {
                    return Engine.eval("env.capture()");
                } catch (ScriptException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        }.start().get());
    }
}
