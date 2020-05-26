package NG.Network;

import NG.Core.Game;
import NG.Entities.Entity;
import NG.GUIMenu.Components.*;
import NG.InputHandling.MouseTools.AbstractMouseTool;
import org.joml.Vector2i;
import org.joml.Vector3fc;
import org.lwjgl.glfw.GLFW;

import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

/**
 * @author Geert van Ieperen created on 22-5-2020.
 */
public class Schedule extends AbstractCollection<NetworkPosition> {
    private Node firstNode;
    private int size;

    /**
     * creates an empty schedule
     */
    public Schedule() {
        firstNode = null;
        size = 0;
    }

    /**
     * Create a schedule consisting of the given network positions
     * @param positions the positions to create a schedule of.
     */
    public Schedule(NetworkPosition... positions) {
        this();
        addAll(Arrays.asList(positions));
    }

    /**
     * creates a schedule from the positions given by the iterator of other. If other is a Schedule, this creates an
     * effective copy of that schedule.
     */
    public Schedule(Collection<NetworkPosition> other) {
        this();
        addAll(other);
    }

    /**
     * appends the given element to the end of the schedule. This change is reflected in all ScheduleNodes and Iterators
     * of this collection.
     * @param element the position to add to the schedule
     * @return true
     */
    public boolean add(NetworkPosition element) {
        if (firstNode == null) {
            firstNode = new Node(element);
            size = 1;

        } else {
            addBetween(firstNode.prev, element, firstNode);
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

    public void addAfter(Node node, NetworkPosition element) {
        if (node == null) {
            add(element);

        } else {
            addBetween(node, element, node.next);
        }
    }

    private void addBetween(Node before, NetworkPosition element, Node next) {
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
    public Iterator<NetworkPosition> iterator() {
        return new Iterator<>() {
            int i = 0;
            Node node = firstNode;

            @Override
            public boolean hasNext() {
                return i < size;
            }

            @Override
            public NetworkPosition next() {
                NetworkPosition result = node.element;
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

    public ScheduleUI getUI(Game game) {
        return new ScheduleUI(game);
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
        public final NetworkPosition element;
        private Node next;
        private Node prev;

        private Node(Node prev, NetworkPosition element, Node next) {
            this.element = element;
            this.next = next;
            this.prev = prev;
        }

        private Node(NetworkPosition element) {
            this.element = element;
            this.next = this;
            this.prev = this;
        }

        @Override
        public String toString() {
            return "Node{" + element + '}';
        }
    }

    public class ScheduleUI extends SFrame {
        private final Game game;
        private Node selectedNode;
        private final SContainer body;

        public ScheduleUI(Game game) {
            super("Schedule");
            this.game = game;
            body = SContainer.singleton(new SFiller());

            setMainPanel(SContainer.column(
                    body,
                    SContainer.row(
                            new SButton("Add Station", this::setAdder),
                            new SButton("Remove Selected", () -> removeNode(selectedNode))
                    )
            ));
            updateBody();
        }

        private void updateBody() {
            SPanel panel = new SPanel(1, size);
            int i = 0;
            for (Node node : nodes()) {
                SExtendedTextArea text = new SExtendedTextArea(node.element.toString(), 50, false);
                text.setClickListener((button, xRel, yRel) -> {
                    if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                        selectedNode = node;
                    }
                });

                panel.add(text, new Vector2i(0, i++));
            }
            body.add(panel, null);
        }

        private void setAdder() {
            game.inputHandling().setMouseTool(new AbstractMouseTool(game) {
                @Override
                public void apply(Entity entity, Vector3fc origin, Vector3fc direction) {
                    if (getMouseAction() == MouseAction.PRESS_ACTIVATE) {
                        if (entity instanceof NetworkPosition) {
                            addAfter(selectedNode, (NetworkPosition) entity);
                            updateBody();
                        }
                    }
                }

                @Override
                public void apply(Vector3fc position, Vector3fc origin, Vector3fc direction) {
                }
            });
        }
    }
}
