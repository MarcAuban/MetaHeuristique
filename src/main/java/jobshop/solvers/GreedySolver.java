package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Solver;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;

import java.util.ArrayList;

public class GreedySolver implements Solver {
    public enum Priority {
        SPT ,
        LRPT ,
        EST_SPT ,
        EST_LRPT
    }
    private Priority param;

    public GreedySolver(Priority param) {
        this.param = param;
    }

    @Override
    public Result solve(Instance instance, long deadline) {

        ResourceOrder resource = new ResourceOrder(instance);

        ArrayList<Task> toDo = new ArrayList<>();
        ArrayList<Task> doable = new ArrayList<>();
        int[] remainingTime = new int[instance.numJobs];
        int[] tasksPerJob = new int[instance.numMachines];

        int[] bestTimeJob = new int[instance.numJobs]; //EST
        for (int i=0; i<instance.numJobs; i++) {
            bestTimeJob[i] = 0;
        }

        int[] bestTimeMachine = new int[instance.numMachines]; //EST
        for (int i=0; i<instance.numMachines; i++) {
            bestTimeMachine[i] = 0;
        }

        for (int i=0; i<instance.numMachines; i++) {
            tasksPerJob[i] = 0;
        }

        for (int i=0; i<instance.numJobs; i++) { //LRPT
            remainingTime[i] = 0;
            for(int j=0; j< instance.numTasks; j++){
                remainingTime[i]+=instance.duration(i,j);
            }
        }

        //doable task : premiere tache de chacun des jobs
        for(int j=0; j<instance.numJobs; j++) {
            for(int t=1; t<instance.numTasks; t++) {
                toDo.add(new Task(j,t));
            }
            doable.add(new Task(j,0));
        }

        Task next = null;
        while(!doable.isEmpty()) {
            switch (this.param) {
                case SPT :
                    next = SPT(doable, instance);
                    break;

                case LRPT :
                    next = LRPT(doable, remainingTime, instance);

                    remainingTime[next.job] -= instance.duration(next);
                    break;

                case EST_SPT :
                    next = EST_SPT(doable, instance, bestTimeJob, bestTimeMachine);

                    bestTimeJob[next.job] += instance.duration(next);
                    bestTimeMachine[instance.machine(next)] += instance.duration(next);
                    break;

                case EST_LRPT :
                    next = EST_LRPT(doable, instance, bestTimeJob, bestTimeMachine, remainingTime);

                    bestTimeJob[next.job] += instance.duration(next);
                    bestTimeMachine[instance.machine(next)] += instance.duration(next);
                    remainingTime[next.job] -= instance.duration(next);
                    break;

                default :
                    System.out.println("ERREUR\n");
                    break;
            }

            int i=0;
            boolean fini = false;
            while(i<toDo.size() && !fini){
                if(toDo.get(i).job == next.job && toDo.get(i).task-1 == next.task){
                    doable.add(toDo.get(i));
                    toDo.remove(i);
                    fini = true;
                }
                i++;
            }

            resource.tasksByMachine[instance.machine(next)][tasksPerJob[instance.machine(next)]]=next;
            tasksPerJob[instance.machine(next)]++;
            doable.remove(next);

            if(deadline - System.currentTimeMillis() < 1){
                return(new Result(instance,resource.toSchedule(),Result.ExitCause.Timeout));
            }
        }
        return new Result(instance, resource.toSchedule(), Result.ExitCause.Blocked);
    }

    public Task SPT(ArrayList<Task> doable, Instance instance) {
        int min = instance.duration(doable.get(0));
        Task minTask = doable.get(0);
        for(Task task : doable) {
            if(instance.duration(task) <min) {
                min = instance.duration(task);
                minTask = task;
            }
        }
        return minTask;
    }

    public Task LRPT(ArrayList<Task> doable, int[] remaining, Instance instance) {
        int maxJob = 0;
        int max = -1;
        Task maxTask = null;

        for (int j=0; j<instance.numJobs; j++) {
            if (remaining[j] > max) {
                max = remaining[j];
                maxJob = j;
            }
        }

        for (Task task : doable) {
            if(task.job == maxJob) {
                maxTask = task;
            }
        }
        return maxTask;
    }

    public int getMaxJobMachine(Task task, Instance instance, int[] bestTimeMachine, int[] bestTimeJob) {
        return Math.max(bestTimeJob[task.job], bestTimeMachine[instance.machine(task)]);
    }

    public Task EST_SPT(ArrayList<Task> doable, Instance instance, int[] bestTimeJob, int[] bestTimeMachine) {
        int min = Integer.MAX_VALUE;
        int EST;
        Task ret = null;
        for (Task curTask : doable) {
            EST = getMaxJobMachine(curTask, instance, bestTimeMachine, bestTimeJob);
            if (min > EST) {
                min = EST;
                ret = curTask;
            }
            else if (min == EST) { //faire SPT
                assert ret != null;
                if (instance.duration(ret) > instance.duration(curTask)) { //if ret plus court
                    ret = curTask;
                }
            }
        }
        if (ret == null) {
            System.out.println("not found");
        }
        return ret;
    }

    public Task EST_LRPT(ArrayList<Task> doable, Instance instance, int[] bestTimeJob, int[] bestTimeMachine, int[] remaining) {
        int min = Integer.MAX_VALUE;
        int EST;
        Task ret = null;
        for (Task curTask : doable) {
            EST = getMaxJobMachine(curTask, instance, bestTimeMachine, bestTimeJob);
            if (min > EST) {
                min = EST;
                ret = curTask;
            }
            else if (min == EST) { //faire SPT
                assert ret != null;
                if (remaining[ret.job] > remaining[curTask.job]) { //if ret plus long
                    ret = curTask;
                }
            }
        }
        if (ret == null) {
            System.out.println("not found");
        }
        return ret;
    }
}