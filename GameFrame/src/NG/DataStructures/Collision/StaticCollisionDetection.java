package NG.DataStructures.Collision;

import NG.DataStructures.Generic.PairList;
import NG.Tools.Logger;
import NG.Tools.Toolbox;
import org.joml.AABBf;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Allows checking overlap of static hitboxes.
 * @author Geert van Ieperen created on 10-3-2018.
 */
public class StaticCollisionDetection {
    private static final int INSERTION_SORT_BOUND = 64;

    private CollisionEntity[] xLowerSorted;
    private CollisionEntity[] yLowerSorted;
    private CollisionEntity[] zLowerSorted;

    public StaticCollisionDetection() {
        this(Collections.emptyList());
    }

    /**
     * Collects the given entities and allows collision and phisics calculations to influence these entities
     * @param entities a list of fixed entities.
     */
    public StaticCollisionDetection(Collection<StaticEntity> entities) {
        int nOfEntities = entities.size();
        xLowerSorted = new CollisionEntity[nOfEntities];
        yLowerSorted = new CollisionEntity[nOfEntities];
        zLowerSorted = new CollisionEntity[nOfEntities];

        populate(entities, xLowerSorted, yLowerSorted, zLowerSorted);
    }

    /**
     * populates the given arrays, and sorts the arrays on the lower coordinate of the hitboxes (x, y and z
     * respectively)
     */
    private void populate(
            Collection<? extends StaticEntity> entities,
            CollisionEntity[] xLowerSorted, CollisionEntity[] yLowerSorted, CollisionEntity[] zLowerSorted
    ) {
        int i = 0;
        for (StaticEntity entity : entities) {
            assert entity != null;
            CollisionEntity asCollisionEntity = new CollisionEntity(entity);
            xLowerSorted[i] = asCollisionEntity;
            yLowerSorted[i] = asCollisionEntity;
            zLowerSorted[i] = asCollisionEntity;
            i++;
        }

        if (entities.size() < INSERTION_SORT_BOUND) {
            Toolbox.insertionSort(xLowerSorted, CollisionEntity::xLower);
            Toolbox.insertionSort(yLowerSorted, CollisionEntity::yLower);
            Toolbox.insertionSort(zLowerSorted, CollisionEntity::zLower);

        } else {
            Arrays.sort(xLowerSorted, (a, b) -> Float.compare(a.xLower(), b.xLower()));
            Arrays.sort(yLowerSorted, (a, b) -> Float.compare(a.yLower(), b.yLower()));
            Arrays.sort(zLowerSorted, (a, b) -> Float.compare(a.zLower(), b.zLower()));
        }
    }

    public List<StaticEntity> getIntersectingEntities(AABBf hitbox) {
        List<StaticEntity> xEntities = checkOverlap(xLowerSorted, hitbox, box -> box.minX, box -> box.maxX);
        List<StaticEntity> yEntities = checkOverlap(yLowerSorted, hitbox, box -> box.minY, box -> box.maxY);
        List<StaticEntity> zEntities = checkOverlap(zLowerSorted, hitbox, box -> box.minZ, box -> box.maxZ);

        xEntities.retainAll(yEntities);
        xEntities.retainAll(zEntities);
        return xEntities;
    }

    /**
     * generate a list (possibly empty) of all pairs of objects that may have collided. This can include (parts of) the
     * ground, but not an object with itself. One pair does not occur the other way around.
     * @return a collection of pairs of objects that are close to each other
     */
    public PairList<CollisionEntity, CollisionEntity> getIntersectingPairs() {
        assert testInvariants();

        CollisionEntity[] entityArray = entityArray();
        int nrOfEntities = entityArray.length;

        // initialize id values to correspond to the array
        for (int i = 0; i < nrOfEntities; i++) {
            entityArray[i].setID(i);
        }

        AdjacencyMatrix adjacencies = new AdjacencyMatrix(nrOfEntities, 3);

        checkOverlap(adjacencies, xLowerSorted, CollisionEntity::xLower, CollisionEntity::xUpper);
        checkOverlap(adjacencies, yLowerSorted, CollisionEntity::yLower, CollisionEntity::yUpper);
        checkOverlap(adjacencies, zLowerSorted, CollisionEntity::zLower, CollisionEntity::zUpper);

        int nrOfElts = adjacencies.nrOfFoundElements();

        PairList<CollisionEntity, CollisionEntity> allEntityPairs = new PairList<>(nrOfElts);
        adjacencies.forEach((i, j) -> allEntityPairs.add(
                entityArray[i], entityArray[j]
        ));

        return allEntityPairs;
    }

    /**
     * iterating over the sorted array, increase the value of all pairs that have coinciding intervals
     * @param adjacencyMatrix the matrix where the pairs are marked using entity id's
     * @param sortedArray     an array sorted increasingly on the lower mapping
     * @param lower           a function that maps to the lower value of the interval of the entity
     * @param upper           a function that maps an entity to its upper interval
     */
    protected void checkOverlap(
            AdjacencyMatrix adjacencyMatrix, CollisionEntity[] sortedArray, Function<CollisionEntity, Float> lower,
            Function<CollisionEntity, Float> upper
    ) {
        // INVARIANT:
        // all items i where i.lower < source.lower, are already added to the matrix

        int nOfItems = sortedArray.length;
        for (int i = 0; i < (nOfItems - 1); i++) {
            CollisionEntity subject = sortedArray[i];

            // increases the checks count of every source with index less than i, with position less than the given minimum
            int j = i + 1;
            CollisionEntity target = sortedArray[j++];

            // while the lowerbound of target is less than the upperbound of our subject
            while (lower.apply(target) <= upper.apply(subject)) {
                adjacencyMatrix.add(target.getID(), subject.getID());

                if (j == nOfItems) break;
                target = sortedArray[j++];
            }
        }
    }

    protected List<StaticEntity> checkOverlap(
            CollisionEntity[] sortedArray, AABBf subject, Function<AABBf, Float> lower,
            Function<AABBf, Float> upper
    ) {
        float targetValue = lower.apply(subject);
        int nrOfItems = sortedArray.length;

        // binary search
        int low = 0;
        int high = nrOfItems - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            CollisionEntity elt = sortedArray[mid];
            float value = lower.apply(elt.hitbox);

            if (value < targetValue) {
                low = mid + 1;

            } else {
                high = mid - 1;

            }
        }

        int j = low + 1;

        List<StaticEntity> entities = new ArrayList<>();
        CollisionEntity target = sortedArray[j++];

        // while the lowerbound of target is less than the upperbound of our subject
        while (lower.apply(target.hitbox) <= upper.apply(subject)) {
            entities.add(target.entity());

            if (j == nrOfItems) break;
            target = sortedArray[j++];
        }

        return entities;
    }

    public void addAll(Collection<StaticEntity> entities) {
        int nOfNewEntities = entities.size();
        if (nOfNewEntities <= 0) return;

        CollisionEntity[] newXSort = new CollisionEntity[nOfNewEntities];
        CollisionEntity[] newYSort = new CollisionEntity[nOfNewEntities];
        CollisionEntity[] newZSort = new CollisionEntity[nOfNewEntities];

        populate(entities, newXSort, newYSort, newZSort);

        xLowerSorted = Toolbox.mergeArrays(xLowerSorted, newXSort, CollisionEntity::xLower);
        yLowerSorted = Toolbox.mergeArrays(yLowerSorted, newYSort, CollisionEntity::yLower);
        zLowerSorted = Toolbox.mergeArrays(zLowerSorted, newZSort, CollisionEntity::zLower);
    }

    public void addEntity(StaticEntity entity) {
        CollisionEntity collisionEntity = new CollisionEntity(entity);
        CollisionEntity[] asArray = {collisionEntity}; // bit of a hack

        xLowerSorted = Toolbox.mergeArrays(xLowerSorted, asArray, CollisionEntity::xLower);
        yLowerSorted = Toolbox.mergeArrays(yLowerSorted, asArray, CollisionEntity::yLower);
        zLowerSorted = Toolbox.mergeArrays(zLowerSorted, asArray, CollisionEntity::zLower);
    }

    /**
     * Remove the selected entities off the entity lists in a robust way. Entities that did not exist are ignored, and
     * doubles are accepted.
     * @param targets a collection of entities to be removed
     */
    public void removeAll(Collection<StaticEntity> targets) {
        xLowerSorted = deleteAll(targets, xLowerSorted);
        yLowerSorted = deleteAll(targets, yLowerSorted);
        zLowerSorted = deleteAll(targets, zLowerSorted);
    }

    private CollisionEntity[] deleteAll(Collection<StaticEntity> targets, CollisionEntity[] array) {
        int xi = 0;
        for (int i = 0; i < array.length; i++) {
            StaticEntity entity = array[i].entity();
            if ((entity != null) && targets.contains(entity)) {
                continue;
            }
            array[xi++] = array[i];
        }
        return Arrays.copyOf(array, xi);
    }

    /**
     * @return an array of the entities, backed by any local representation. Should only be used for querying, otherwise
     * it must be cloned
     */
    private CollisionEntity[] entityArray() {
        return xLowerSorted;
    }

    public Collection<StaticEntity> getEntityList() {
        CollisionEntity[] eties = entityArray();
        ArrayList<StaticEntity> elts = new ArrayList<>(eties.length);

        for (CollisionEntity ety : eties) {
            elts.add(ety.entity());
        }

        return elts;
    }

    public boolean contains(StaticEntity entity) {
        for (CollisionEntity ety : entityArray()) {
            if (ety.entity().equals(entity)) return true;
        }

        return false;
    }

    public void forEach(Consumer<StaticEntity> action) {
        for (CollisionEntity ety : entityArray()) {
            action.accept(ety.entity());
        }
    }

    public void removeIf(Predicate<StaticEntity> guard) {
        List<StaticEntity> targets = new ArrayList<>();

        for (CollisionEntity ety : entityArray()) {
            StaticEntity entity = ety.entity();
            if (guard.test(entity)) {
                targets.add(entity);
            }
        }

        removeAll(targets);
    }

    public synchronized void cleanup() {
        xLowerSorted = new CollisionEntity[0];
        yLowerSorted = new CollisionEntity[0];
        zLowerSorted = new CollisionEntity[0];
    }

    /**
     * tracks how often pairs of integers are added, and allows querying whether a given pair has been added at least a
     * given number of times.
     */
    private static class AdjacencyMatrix {
        // this map is indexed using the entity id values, with i > j
        // if (adjacencyMatrix[i][j] == n) then entityArray[i] and entityArray[j] have n coordinates with coinciding intervals
        Map<Integer, Map<Integer, Integer>> relations;
        Map<Integer, Set<Integer>> found;
        private int depth;

        /**
         * @param nrOfElements maximum value that can be added
         * @param depth        how many times a number must be added to trigger {@link #has(int, int)}. For 3-coordinate
         *                     matching, use 3
         */
        public AdjacencyMatrix(int nrOfElements, int depth) {
            relations = new HashMap<>(nrOfElements);
            found = new HashMap<>();
            this.depth = depth;
        }

        public void add(int i, int j) {
            if (j > i) {
                int t = i;
                i = j;
                j = t;
            }

            Map<Integer, Integer> firstSide = relations.computeIfAbsent(i, HashMap::new);
            int newValue = firstSide.getOrDefault(j, 0) + 1;
            if (newValue == depth) {
                found.computeIfAbsent(i, HashSet::new).add(j);
            }
            firstSide.put(j, newValue);
        }

        public boolean has(int i, int j) {
            return found.containsKey(i) && found.get(i).contains(j);
        }

        public void forEach(BiConsumer<Integer, Integer> action) {
            for (Integer i : found.keySet()) {
                for (Integer j : found.get(i)) {
                    action.accept(i, j);
                }
            }
        }

        public int nrOfFoundElements() {
            int count = 0;
            for (Set<Integer> integers : found.values()) {
                count += integers.size();
            }
            return count;
        }
    }

    /**
     * tests whether the invariants holds. Throws an error if any of the arrays is not correctly sorted or any other
     * assumption no longer holds
     */
    boolean testInvariants() {
        String source = Logger.getCallingMethod(1);
        Logger.DEBUG.printSpamless(source, "\n    " + source + " Checking collision detection invariants");

        // all arrays contain all entities
        Set<StaticEntity> allEntities = new HashSet<>();
        for (CollisionEntity colEty : entityArray()) {
            allEntities.add(colEty.entity());
        }

        for (CollisionEntity collEty : xLowerSorted) {
            if (!allEntities.contains(collEty.entity())) {
                throw new IllegalStateException("Array x does not contain entity " + collEty.entity());
            }
        }
        for (CollisionEntity collEty : yLowerSorted) {
            if (!allEntities.contains(collEty.entity())) {
                throw new IllegalStateException("Array y does not contain entity " + collEty.entity());
            }
        }
        for (CollisionEntity collEty : zLowerSorted) {
            if (!allEntities.contains(collEty.entity())) {
                throw new IllegalStateException("Array z does not contain entity " + collEty.entity());
            }
        }

        // all arrays are of equal length
        if ((xLowerSorted.length != yLowerSorted.length) || (xLowerSorted.length != zLowerSorted.length)) {
            Logger.ERROR.print(Arrays.toString(entityArray()));
            throw new IllegalStateException("Entity arrays have different lengths: "
                    + xLowerSorted.length + ", " + yLowerSorted.length + ", " + zLowerSorted.length
            );
        }

        // x is sorted
        float init = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < xLowerSorted.length; i++) {
            CollisionEntity collisionEntity = xLowerSorted[i];
            if (collisionEntity.xLower() < init) {
                Logger.ERROR.print("Sorting error on x = " + i);
                Logger.ERROR.print(Arrays.toString(xLowerSorted));
                throw new IllegalStateException("Sorting error on x = " + i);
            }
            init = collisionEntity.xLower();
        }

        // y is sorted
        init = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < yLowerSorted.length; i++) {
            CollisionEntity collisionEntity = yLowerSorted[i];
            if (collisionEntity.yLower() < init) {
                Logger.ERROR.print("Sorting error on y = " + i);
                Logger.ERROR.print(Arrays.toString(yLowerSorted));
                throw new IllegalStateException("Sorting error on y = " + i);
            }
            init = collisionEntity.yLower();
        }

        // z is sorted
        init = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < zLowerSorted.length; i++) {
            CollisionEntity collisionEntity = zLowerSorted[i];
            if (collisionEntity.zLower() < init) {
                Logger.ERROR.print("Sorting error on z = " + i);
                Logger.ERROR.print(Arrays.toString(zLowerSorted));
                throw new IllegalStateException("Sorting error on z = " + i);
            }
            init = collisionEntity.zLower();
        }

        return true;
    }

}
