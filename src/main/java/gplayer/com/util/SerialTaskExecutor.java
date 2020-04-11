/**
*Copyright (C) 2019 Ganiyu Emilandu
*
*This program is free software: you can redistribute it and/or modify
*it under the terms of the GNU General Public License as published by
*the Free Software Foundation, either version 3 of the License, or
*(at your option) any later version.
*
*THIS PROGRAM IS DISTRIBUTED IN THE HOPE THAT IT WILL BE USEFUL,
*BUT WITHOUT ANY WARRANTY; WITHOUT EVEN THE IMPLIED WARRANTY OF
*MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.
*SEE THE GNU GENERAL PUBLIC LICENSE for MORE DETAILS.
*
*You should have received a copy of the GNU General Public License along with this program. If not, see
*<https://www.gnu.org/licenses/>.
*
*/

package gplayer.com.util;

/**Spins a thread in the background
*that accepts runnables to be executed serially in order of input.
*/

public final class SerialTaskExecutor implements Runnable {
    private java.util.concurrent.ConcurrentLinkedQueue<Runnable> queue = new java.util.concurrent.ConcurrentLinkedQueue<>();
    private Thread executor;
    private volatile boolean started = false;
    private volatile boolean suspended = true;

    /**Creates a new instance of this class.*/
    public SerialTaskExecutor() {
        this(null);
    }

    /**Creates a new instance of this class.
    *@param name
    *the name to associate with the spawned thread.
    */
    public SerialTaskExecutor(String name) {
        executor = (name == null)? new Thread(this): new Thread(this, name);
        executor.setDaemon(true);
    }

    /**Inserts a task into the Runnable queue
    *to be run at an unspecified time in the future.
    */
    public final void run(Runnable task) {
        if (task == null)
            return;
        queue.add(task);
        if (suspended)
            resume();
    }

    /**Clears the queue to prevent any pending task from being executed.*/
    public final void dispose() {
        queue.clear();
    }

    public final synchronized void resume() {
        suspended = false;
        if (!started) {
            started = true;
            executor.start();
        }
        else {
            notify();
        }
    }

    public final synchronized void suspend() {
        suspended = true;
    }

    private synchronized void trySuspend() {
        synchronized (this) {
            while (suspended) {
                try {
                    wait();
                }
                catch (InterruptedException ex) {}
            }
        }
    }

    @Override
    public void run() {
        while (true) {
            Runnable task = queue.poll();
            if (task == null)
                suspend();
            else
                task.run();
            trySuspend();
        }
    }
}