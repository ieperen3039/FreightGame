package NG.Network;

import NG.Core.Game;
import NG.Entities.Entity;
import NG.GUIMenu.Components.*;
import NG.GUIMenu.Menu.EntityActionTool;
import NG.GUIMenu.Menu.MainMenu;
import org.joml.Vector2i;
import org.lwjgl.glfw.GLFW;

import java.util.AbstractCollection;
import java.util.Iterator;

/**
 * @author Geert van Ieperen created on 22-5-2020.
 */
public class Schedule extends AbstractCollection<ScheduleElement> {
    private Node firstNode;
    private int size;

    /**
     * creates an empty schedule
     */
    public Schedule() {
        firstNode = null;
        size = 0;
    }

    public Schedule(Schedule other) {
        this();
        addAll(other);
    }

    /**
     * appends the given target to the end of the schedule. This change is reflected in all ScheduleNodes and Iterators
     * of this collection.
     * @param target the position to add to the schedule
     * @return true
     */
    public boolean add(NetworkPosition target) {
        return add(new ScheduleElement(target));
    }

    public boolean add(ScheduleElement newElement) {
        if (firstNode == null) {
            firstNode = new Node(newElement);
            size = 1;

        } else {
            addBetween(firstNode.prev, newElement, firstNode);
        }

        return true;
    }

    @Override
    public boolean remove(Object o) {
        if (o instanceof NetworkPosition) {
            for (Node node : nodes()) {
                if (node.element.equals(o)) {
                    removeNode(node);
                    return true;
                }
            }

        } else if (o instanceof Node) { // this is allowed:
            removeNode((Node) o);
        }

        return false;
    }

    public void addAfter(Node node, ScheduleElement element) {
        if (node == null) {
            add(element);

        } else {
            addBetween(node, element, node.next);
        }
    }

    private void addBetween(Node before, ScheduleElement element, Node next) {
        Node newNode = new Node(before, element, next);
        before.next = newNode;
        next.prev = newNode;
        size++;
    }

    public Node getFirstNode() {
        return firstNode;
    }

    public Node getNextNode(Node node) {
        return node.next;
    }

    public Node getPreviousNode(Node node) {
        return node.prev;
    }

    public Iterable<Node> nodes() {
        return () -> new Iterator<>() {
            int i = 0;
            Node node = firstNode;

            @Override
            public boolean hasNext() {
                return i < size;
            }

            @Override
            public Node next() {
                Node result = node;
                node = node.next;
                i++;
                return result;
            }
        };
    }

    @Override
    public Iterator<ScheduleElement> iterator() {
        return new Iterator<>() {
            int i = 0;
            Node node = firstNode;

            @Override
            public boolean hasNext() {
                return i < size;
            }

            @Override
            public ScheduleElement next() {
                ScheduleElement result = node.element;
                node = node.next;
                i++;
                return result;
            }
        };
    }

    @Override
    public int size() {
        return size;
    }

    /**
     * removes the given node. This change is reflected in all ScheduleNodes and Iterators of this collection.
     * @param node the node to remove.
     */
    public void removeNode(Node node) {
        if (node.next == node) {
            firstNode = null;

        } else {
            node.next.prev = node.prev;
            node.prev.next = node.next;
        }

        size--;
    }

    public static class Node {
        public final ScheduleElement element;
        private Node next;
        private Node prev;

        private Node(Node prev, ScheduleElement element, Node next) {
            this.element = element;
            this.next = next;
            this.prev = prev;
        }

        private Node(ScheduleElement element) {
            this.element = element;
            this.next = this;
            this.prev = this;
        }

        @Override
        public String toString() {
            return "Node{" + element + '}';
        }
    }

    public static class ScheduleUI extends SFrame {
        private final Game game;
        private final Schedule schedule;
        private final SContainer body;
        private Node selectedNode;

        public ScheduleUI(Game game, Schedule schedule) {
            super("Schedule");
            this.game = game;
            this.schedule = schedule;
            body = SContainer.singleton(new SFiller());

            setMainPanel(SContainer.column(
                    body,
                    SContainer.row(
                            new SButton("Add Station", this::activateAddTool),
                            new SButton("Remove", this::removeSelected)
                    )
            ));
            updateBody();
        }

        private void updateBody() {
            SPanel panel = new SPanel(1, schedule.size);
            int i = 0;
            for (Node node : schedule.nodes()) {
                SExtendedTextArea text = new SExtendedTextArea(node.element.toString(), MainMenu.TEXT_PROPERTIES);
                text.setClickListener((button, xRel, yRel) -> {
                    if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                        selectedNode = node;
                    }
                });

                panel.add(text, new Vector2i(0, i++));
            }
            body.add(panel, null);
        }

        protected void removeSelected() {
            if (schedule.isEmpty()) return;
            schedule.removeNode(selectedNode == null ? schedule.firstNode.prev : selectedNode);
            updateBody();
        }

        private void activateAddTool() {
            EntityActionTool tool = new EntityActionTool(game, entity -> entity instanceof NetworkPosition, this::add);
            game.inputHandling().setMouseTool(tool);
        }

        private void add(Entity entity) {
            ScheduleElement newElement = new ScheduleElement((NetworkPosition) entity);

            schedule.addAfter(selectedNode, newElement);
            updateBody();
        }
    }

    /**
     * @author Geert van Ieperen created on 14-6-2020.
     */
    public interface UpdateListener {
        void onScheduleUpdate(NetworkPosition element);
    }
}
