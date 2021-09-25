package chemotaxis.g11;

import java.awt.Point;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.HashMap;

import chemotaxis.sim.ChemicalPlacement;
import chemotaxis.sim.ChemicalCell;
import chemotaxis.sim.SimPrinter;
import chemotaxis.sim.DirectionType;

public class Controller extends chemotaxis.sim.Controller {
    DirectionType[][] directionMap;
    int[][] steps;
    //HashMap<Point, DirectionType> agents;
    HashMap<Point, DirectionType> onConveyerAgents;
    Point start;
    Point target;

    int chemicalsPerAgent;

    /**
     * Controller constructor
     *
     * @param start       start cell coordinates
     * @param target      target cell coordinates
     * @param size     	 grid/map size
     * @param simTime     simulation time
     * @param budget      chemical budget
     * @param seed        random seed
     * @param simPrinter  simulation printer
     *
     */
    public Controller(Point start, Point target, Integer size, ChemicalCell[][] grid, Integer simTime, Integer budget, Integer seed, SimPrinter simPrinter, Integer agentGoal, Integer spawnFreq) {
        super(start, target, size, grid, simTime, budget, seed, simPrinter, agentGoal, spawnFreq);
        onConveyerAgents = new HashMap<>();
        this.start = start;
        this.target = target;
        int endX = target.x - 1;
        int endY = target.y - 1;
        boolean[][] visited = new boolean[size][size];
        steps = new int[size][size];
        directionMap = new DirectionType[size][size];
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                visited[r][c] = false;
                directionMap[r][c] = DirectionType.CURRENT;
                steps[r][c] = 0;
            }
        }
        Queue<Point> queue = new LinkedList<Point>();
        queue.add(new Point(endX, endY));

        visited[endX][endY] = true;
        directionMap[endX][endY] = DirectionType.CURRENT;
        while (!queue.isEmpty()) {
            Point curr = queue.remove();
            int x = curr.x;
            int y = curr.y;
            helper(x + 1, y, 1, 0, DirectionType.NORTH, steps[x][y] + 1, queue, visited);
            helper(x - 1, y, -1, 0, DirectionType.SOUTH, steps[x][y] + 1, queue, visited);
            helper(x, y + 1, 0, 1, DirectionType.WEST, steps[x][y] + 1, queue, visited);
            helper(x, y - 1, 0, -1, DirectionType.EAST, steps[x][y] + 1, queue, visited);
        }
        //Prints the map that is made
        /*
        HashMap<DirectionType, Character> debugging = new HashMap<>();
        debugging.put(DirectionType.NORTH, 'N');
        debugging.put(DirectionType.SOUTH, 'S');
        debugging.put(DirectionType.EAST, 'E');
        debugging.put(DirectionType.WEST, 'W');
        debugging.put(DirectionType.CURRENT, 'C');
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                System.out.print(debugging.get(directionMap[r][c]));
            }
            System.out.println();
        }
        System.out.println("Steps Map");
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                System.out.print(steps[r][c]);
            }
            System.out.println();
        }
        */
        int alpha = agentGoal;
        chemicalsPerAgent = (budget - alpha) / agentGoal;
    }

    public int closestToTarget(ArrayList<Point> locations) {
        int closestDistance = 9999999;
        int closestIdx = 0;
        for(int i = 0; i < locations.size(); i++) {
            int x = locations.get(i).x;
            int y = locations.get(i).y;
            int distance = Math.abs(x - this.target.x) + Math.abs(y - this.target.y);
            if(distance > 0 && distance < closestDistance) {
                closestIdx = i;
                closestDistance = distance;
            }
        }
        return closestIdx;
    }

    /**
     * Apply chemicals to the map
     *
     * @param currentTurn         current turn in the simulation
     * @param chemicalsRemaining  number of chemicals remaining
     * @param locations           current locations of the agents
     * @param grid                game grid/map
     * @return                    a cell location and list of chemicals to apply
     *
     */
    @Override
    public ChemicalPlacement applyChemicals(Integer currentTurn, Integer chemicalsRemaining, ArrayList<Point> locations, ChemicalCell[][] grid) {
        ChemicalPlacement chemicalPlacement = new ChemicalPlacement();

        HashMap<Point, DirectionType> newAgents = new HashMap<Point, DirectionType>();

        if (locations.contains(target)) {
            onConveyerAgents.remove(target);
            locations.remove(target);
        }

        boolean placeChemical = false;

        for(Point p : locations) {
            if(!onConveyerAgents.containsKey(p) && steps[p.x - 1][p.y - 1] <= chemicalsPerAgent) {
                onConveyerAgents.put(p, null);
            }
            if (onConveyerAgents.containsKey(p)) {
                if (onConveyerAgents.get(p) != directionMap[p.x - 1][p.y - 1]) {
                    chemicalPlacement.location = getChemicalPlacement(p.x - 1, p.y - 1, p);
                    onConveyerAgents.replace(p, directionMap[p.x - 1][p.y - 1]);
                    placeChemical = true;
                    break;
                }
            }
        }

        /*
        for(Point p : onConveyerAgents.keySet()) {
            System.out.println("MARKER");
            System.out.println(p);
            System.out.println(onConveyerAgents.get(p));
        }
         */


        for (Point p: onConveyerAgents.keySet()) {
            DirectionType currentDirection = onConveyerAgents.get(p);
            if (currentDirection == DirectionType.NORTH) {
                newAgents.put(new Point(p.x - 1, p.y), DirectionType.NORTH);
            }
            else if (currentDirection == DirectionType.SOUTH) {
                newAgents.put(new Point(p.x + 1, p.y), DirectionType.SOUTH);
            }
            else if (currentDirection == DirectionType.WEST) {
                newAgents.put(new Point(p.x, p.y - 1), DirectionType.WEST);
            }
            else if (currentDirection == DirectionType.EAST) {
                newAgents.put(new Point(p.x, p.y + 1), DirectionType.EAST);
            }
            else {
                newAgents.put(new Point(p.x, p.y), DirectionType.CURRENT);
            }
        }

        onConveyerAgents = newAgents;
        List<ChemicalCell.ChemicalType> chemicals = new ArrayList<>();

        if(placeChemical) {
            chemicals.add(ChemicalCell.ChemicalType.BLUE);
        }

        chemicalPlacement.chemicals = chemicals;
        return chemicalPlacement;


        /*
        if (locations.contains(start)) {
            agents.put(start, DirectionType.SOUTH);
        }

        if (locations.contains(target)) {
            agents.remove(target);
            locations.remove(target);
        }

        Point wrongDirectionAgent = null;
        double threshold = 0.1;
        for (Point p: locations) {
            if (!p.equals(target) && agents.get(p) != directionMap[p.x - 1][p.y - 1]) {
                if (grid[p.x - 1][p.y - 1].getConcentration(ChemicalCell.ChemicalType.BLUE) < threshold &&
                        grid[p.x - 1][p.y - 1].getConcentration(ChemicalCell.ChemicalType.GREEN) < threshold &&
                        grid[p.x - 1][p.y - 1].getConcentration(ChemicalCell.ChemicalType.RED) < threshold) {
                    wrongDirectionAgent = p;
                    break;
                }
            }
        }

        List<ChemicalCell.ChemicalType> chemicals = new ArrayList<>();
        if (wrongDirectionAgent != null) {
            DirectionType newDirection = directionMap[wrongDirectionAgent.x - 1][wrongDirectionAgent.y - 1];
            if (newDirection == DirectionType.NORTH) {
                chemicalPlacement.location = new Point(wrongDirectionAgent.x - 1, wrongDirectionAgent.y);
                agents.put(wrongDirectionAgent, DirectionType.NORTH);
                chemicals.add(ChemicalCell.ChemicalType.RED);
            }
            else if (newDirection == DirectionType.SOUTH) {
                chemicalPlacement.location = new Point(wrongDirectionAgent.x + 1, wrongDirectionAgent.y);
                agents.put(wrongDirectionAgent, DirectionType.SOUTH);
                chemicals.add(ChemicalCell.ChemicalType.BLUE);
            }
            else if (newDirection == DirectionType.EAST) {
                chemicalPlacement.location = new Point(wrongDirectionAgent.x, wrongDirectionAgent.y + 1);
                agents.put(wrongDirectionAgent, DirectionType.EAST);
                chemicals.add(ChemicalCell.ChemicalType.GREEN);
            }
            else if (newDirection == DirectionType.WEST) {
                chemicalPlacement.location = new Point(wrongDirectionAgent.x, wrongDirectionAgent.y - 1);
                agents.put(wrongDirectionAgent, DirectionType.WEST);
                chemicals.add(ChemicalCell.ChemicalType.BLUE);
                chemicals.add(ChemicalCell.ChemicalType.GREEN);
            }
            else {
                chemicalPlacement.location = new Point(wrongDirectionAgent.x, wrongDirectionAgent.y);
                agents.put(wrongDirectionAgent, DirectionType.CURRENT);
            }
        }

        HashMap<Point, DirectionType> newAgents = new HashMap<Point, DirectionType>();
        for (Point p: agents.keySet()) {
            DirectionType currentDirection = agents.get(p);
            if (currentDirection == DirectionType.NORTH) {
                newAgents.put(new Point(p.x - 1, p.y), DirectionType.NORTH);
            }
            else if (currentDirection == DirectionType.SOUTH) {
                newAgents.put(new Point(p.x + 1, p.y), DirectionType.SOUTH);
            }
            else if (currentDirection == DirectionType.WEST) {
                newAgents.put(new Point(p.x, p.y - 1), DirectionType.WEST);
            }
            else if (currentDirection == DirectionType.EAST) {
                newAgents.put(new Point(p.x, p.y + 1), DirectionType.EAST);
            }
            else {
                newAgents.put(new Point(p.x, p.y), DirectionType.CURRENT);
            }
        }
        this.agents = newAgents;
         */
        /*
        List<ChemicalCell.ChemicalType> chemicals = new ArrayList<>();
        if (wrongDirectionAgent != null) {
            chemicals.add(ChemicalCell.ChemicalType.BLUE);
        }
         */
        //chemicalPlacement.chemicals = chemicals;
        //return chemicalPlacement;
    }

    private void helper(int x, int y, int xDiff, int yDiff, DirectionType d, int count, Queue<Point> queue, boolean[][] visited) {
        while (x >= 0 && x < size && y >= 0 && y < size && grid[x][y].isOpen() && !visited[x][y]) {
            queue.add(new Point(x , y));
            visited[x][y] = true;
            directionMap[x][y] = d;
            steps[x][y] = count;
            x += xDiff;
            y += yDiff;
        }
    }

    private Point getChemicalPlacement(int currX, int currY, Point agentLocation) {
        if(directionMap[currX][currY] == DirectionType.NORTH) {
            return new Point(agentLocation.x - 1, agentLocation.y);
        }
        else if(directionMap[currX][currY] == DirectionType.SOUTH) {
            return new Point(agentLocation.x + 1, agentLocation.y);
        }
        else if(directionMap[currX][currY] == DirectionType.EAST) {
            return new Point(agentLocation.x, agentLocation.y + 1);
        }
        else if(directionMap[currX][currY] == DirectionType.WEST) {
            return new Point(agentLocation.x, agentLocation.y - 1);
        }
        return null;
    }
}