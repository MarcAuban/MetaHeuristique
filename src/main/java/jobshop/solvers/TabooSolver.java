package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.encodings.ResourceOrder;

import java.util.ArrayList;
import java.util.List;

public class TabooSolver  extends DescentSolver {

    /* Classes héritées
     * static class Block : machine, firstTask, lastTask
     * static class Swap : machine, t1, t2, applyOn
     */

    /* methodes héritées :
     *public Lis <Block> blocksOfCriticalPath(ResourceOrder order)
     *List<Swap> neighbors(Block block)
     */

    private int[][][] sTaboo; // matrice contenant les Swap interdit (et leur temps) : le Swap de la tache i avec j est OK si ( sTaboo[machine][i][j] > k + dureeTaboo )

    private int dureeTaboo=2;
    private int maxIter =100;

    public TabooSolver (int duree, int max) {
        this.dureeTaboo = duree;
        this.maxIter = max;
    }

    //solve
    @Override
    public Result solve(Instance instance, long deadline) {
        GreedySolver solverGreedy = new GreedySolver(GreedySolver.Priority.EST_SPT);
        Result solution = solverGreedy.solve(instance, deadline);
        Result best = solution;
        Result solutionCourante = solution;

        int i = 0;

        //initialisation des solutions tabou
        this.sTaboo = new int[instance.numMachines][instance.numJobs][instance.numJobs];

        //exploration du voisinnage
        while (i < this.maxIter && (deadline - System.currentTimeMillis() > 1) ) {
            // ---------------------------VOISINAGE------------------------------
            //creation du voisinnage de la solution courante (recherche de block et swap)
            ResourceOrder order = new ResourceOrder(solutionCourante.schedule);
            List<Block> blocks = blocksOfCriticalPath(order);
            List <Swap> voisinage = new ArrayList<>();
            for (Block block : blocks) {
                voisinage.addAll(neighbors(block));
            }

            for (Swap s : voisinage) {
                ResourceOrder order2 = new ResourceOrder(solutionCourante.schedule);
                s.applyOn(order2);
            }
            // ------------------------- BOUCLE --------------------------------
            // trouver le meilleur voisin (comparer selon makespan) et mettre a jour
            for (Swap s : voisinage) {
                if (sTaboo[s.machine][s.t1][s.t2] <= i) { //si le swap est autorisé
                    //verification : on ne prend pas les solutions impossibles
                    //creation de la solution
                    ResourceOrder order2 = new ResourceOrder(solutionCourante.schedule);
                    s.applyOn(order2);
                    Result r = new Result(solutionCourante.instance, order2.toSchedule(), solutionCourante.cause);
                    //ajouter r à sTaboo
                    this.sTaboo[s.machine][s.t1][s.t2] = i + dureeTaboo;
                    if (r.schedule != null) { //verification : on ne prend pas les solutions impossibles
                        solutionCourante = r;
                        if (r.schedule.makespan() < best.schedule.makespan()) {
                            best = r;
                        }
                    }
                }
            }
            i = i+1; // incrementation
        }
        return best;
    }

}

