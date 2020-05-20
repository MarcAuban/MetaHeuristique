package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Schedule;
import jobshop.Solver;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;

import java.util.ArrayList;
import java.util.List;

public class DescentSolver implements Solver {

    /** A block represents a subsequence of the critical path such that all tasks in it execute on the same machine.
     * This class identifies a block in a ResourceOrder representation.
     *
     * Consider the solution in ResourceOrder representation
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (0,2) (2,1) (1,1)
     * machine 2 : ...
     *
     * The block with : machine = 1, firstTask= 0 and lastTask = 1
     * Represent the task sequence : [(0,2) (2,1)]
     *
     * */
    static class Block {
        /** machine on which the block is identified */
        final int machine;
        /** index of the first task of the block */
        final int firstTask;
        /** index of the last task of the block */
        final int lastTask;

        Block(int machine, int firstTask, int lastTask) {
            this.machine = machine;
            this.firstTask = firstTask;
            this.lastTask = lastTask;
        }
    }

    /**
     * Represents a swap of two tasks on the same machine in a ResourceOrder encoding.
     *
     * Consider the solution in ResourceOrder representation
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (0,2) (2,1) (1,1)
     * machine 2 : ...
     *
     * The swam with : machine = 1, t1= 0 and t2 = 1
     * Represent inversion of the two tasks : (0,2) and (2,1)
     * Applying this swap on the above resource order should result in the following one :
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (2,1) (0,2) (1,1)
     * machine 2 : ...
     */
    static class Swap {
        // machine on which to perform the swap
        final int machine;
        // index of one task to be swapped
        final int t1;
        // index of the other task to be swapped
        final int t2;

        Swap(int machine, int t1, int t2) {
            this.machine = machine;
            this.t1 = t1;
            this.t2 = t2;
        }

        /** Apply this swap on the given resource order, transforming it into a new solution. */
        public void applyOn(ResourceOrder order) {
            Task swap = order.tasksByMachine[machine][t1];
            order.tasksByMachine[machine][t1] = order.tasksByMachine[machine][t2];
            order.tasksByMachine[machine][t2] = swap;
        }
    }

    @Override
    public Result solve(Instance instance, long deadline) {

        deadline = deadline + System.currentTimeMillis();
        GreedySolver greedySolver = new GreedySolver(GreedySolver.Priority.EST_LRPT);

        Result r = greedySolver.solve(instance, deadline);

        ResourceOrder rso = new ResourceOrder(r.schedule);

        ResourceOrder optimalRso = rso.copy();
        int optimalMakespan = optimalRso.toSchedule().makespan();

        boolean newVoisin = true;

        while (newVoisin && (deadline - System.currentTimeMillis() > 1)) {

            newVoisin=false;

            ResourceOrder temp = optimalRso.copy();

            List<Swap> alSwap = new ArrayList<>();
            List<Block> alBlock = this.blocksOfCriticalPath(temp);
            for (Block block : alBlock) {
                alSwap.addAll(this.neighbors(block));
            }

            for (Swap swap : alSwap) {

                ResourceOrder current = temp.copy();

                swap.applyOn(current);
                Schedule newSch = current.toSchedule();

                if (newSch != null) {
                    int makespanCurrent = newSch.makespan();
                    if (optimalMakespan > makespanCurrent) {
                        optimalRso = current;
                        optimalMakespan = makespanCurrent;
                        newVoisin = true;
                    }
                }
            }
        }
        return new Result(instance, optimalRso.toSchedule(), Result.ExitCause.Blocked);
    }

    int indexTaskOnMachine(ResourceOrder rso, int machine, Task taskObj) {

        int indexTask = -1;

        for (int i = 0; i < rso.instance.numJobs; i++) {
            if (rso.tasksByMachine[machine][i].equals(taskObj)) {
                indexTask = i;
            }
        }
        return indexTask;
    }

    /**
     * Returns a list of all blocks of the critical path.
     */
    List<Block> blocksOfCriticalPath(ResourceOrder order) {

        Schedule schedule = order.toSchedule();

        List<Task> listTask = schedule.criticalPath();

        int indexDebut = -1;
        int indexFin;
        int currentMachine = -1;

        ArrayList<Block> alBlock = new ArrayList<>();

        for (int j = 0; j < listTask.size() - 1; j++) {
            int task = listTask.get(j).task;
            int job = listTask.get(j).job;
            int machine = order.instance.machine(job, task);
            int indexTask = indexTaskOnMachine(order, machine, listTask.get(j));

            if (currentMachine == -1 || currentMachine != machine) {
                currentMachine = machine;
                indexDebut = indexTask;
            }

            int nextTask = listTask.get(j + 1).task;
            int nextJob = listTask.get(j + 1).job;
            int nextMachine = order.instance.machine(nextJob, nextTask);
            int nextIndexTask = indexTaskOnMachine(order, nextMachine, listTask.get(j + 1));

            if (nextMachine != machine) {
                indexFin = indexTask;
                if (indexDebut != indexFin) {
                    alBlock.add(new Block(machine, indexDebut, indexFin));
                }
            } else if (j == listTask.size() - 2) {
                indexFin = nextIndexTask;
                if (indexDebut != indexFin) {
                    alBlock.add(new Block(machine, indexDebut, indexFin));
                }
            }
        }
        return alBlock;
    }

    /**
     * For a given block, return the possible swaps for the Nowicki and Smutnicki neighborhood
     */
    List<Swap> neighbors(Block block) {

        int machine = block.machine;
        int firstTask = block.firstTask;
        int lastTask = block.lastTask;

        ArrayList<Swap> alSwap = new ArrayList<>();

        if(lastTask-firstTask == 1){
            alSwap.add(new Swap(machine,lastTask,firstTask));
        }else{
            alSwap.add(new Swap(machine,firstTask,firstTask+1));
            alSwap.add(new Swap(machine,lastTask-1,lastTask));
        }
        return alSwap;
    }

}
