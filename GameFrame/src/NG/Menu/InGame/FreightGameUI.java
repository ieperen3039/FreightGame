package NG.Menu.InGame;

import NG.Core.Game;
import NG.Core.ModLoader;
import NG.GUIMenu.Components.*;
import NG.GUIMenu.LayoutManagers.GridLayoutManager;
import NG.Menu.InGame.Build.BuildMenu;
import NG.Network.NetworkNode;
import NG.Network.RailNode;
import NG.Tools.Directory;
import NG.Tools.Logger;
import NG.Tracks.TrackPiece;
import org.joml.Vector2i;

import java.util.stream.Stream;

import static NG.Menu.Main.MainMenu.BUTTON_PROPERTIES_STATIC;
import static NG.Menu.Main.MainMenu.BUTTON_PROPERTIES_STRETCH;

/**
 * @author Geert van Ieperen created on 2-9-2020.
 */
public class FreightGameUI extends SContainer.GhostContainer {
    public FreightGameUI(Game game, ModLoader modLoader) {
        super(new GridLayoutManager(1, 3));
        SToolBar toolBar = new SToolBar(game, true);

        toolBar.addButton(
                "Build Object",
                () -> game.gui().addFrame(new BuildMenu(game))
        );

        toolBar.addButton("Options", () -> game.gui().addFrame(
                new SFrame("Options", SContainer.column(
                        new SToggleButton("Show CollisionBox", BUTTON_PROPERTIES_STRETCH, game.settings().RENDER_COLLISION_BOX)
                                .addStateChangeListener((active -> game.settings().RENDER_COLLISION_BOX = active)),

                        new SButton("Dump Network", // find any networknode, and print getNetworkAsString
                                () -> game.state().entities().stream()
                                        .filter(e -> e instanceof TrackPiece)
                                        .map(e -> (TrackPiece) e)
                                        .filter(e -> !e.isDespawnedAt(game.timer().getGameTime()))
                                        .map(TrackPiece::getStartNode)
                                        .map(RailNode::getNetworkNode)
                                        .filter(NetworkNode::isNetworkCritical)
                                        .findAny()
                                        .ifPresentOrElse(
                                                n -> Logger.INFO.print(NetworkNode.getNetworkAsString(n)),
                                                () -> Logger.INFO.print("No network present")
                                        )
                        ),

                        new SButton("Check Network", // checks the NetworkNodes of all track pieces
                                () -> game.state().entities().stream()
                                        .filter(e -> e instanceof TrackPiece)
                                        .map(e -> (TrackPiece) e)
                                        .filter(e -> !e.isDespawnedAt(game.timer().getGameTime()))
                                        .peek(t -> {
                                            if (!t.isValid()) throw new IllegalStateException(String.valueOf(t));
                                        })
                                        .flatMap(t -> Stream.of(t.getStartNode(), t.getEndNode()))
                                        .distinct()
                                        .map(RailNode::getNetworkNode)
                                        .forEach(NetworkNode::check)
                        ),

                        new SButton("dump light map",
                                () -> game.executeOnRenderThread(
                                        () -> game.lights().dumpShadowMap(Directory.screenshots)
                                )
                        )
                ))
        ));

        toolBar.addButton("Exit", () -> {
            game.gui().clear();
            game.gui().setMainGUI(this);
            modLoader.stopGame();
        });

        add(toolBar, new Vector2i(0, 0));
        add(new SFiller(), new Vector2i(0, 1));
        add(new SPanel(SContainer.row(
                new SComponentArea(BUTTON_PROPERTIES_STRETCH),
                new SActiveTextArea(() -> "$" + game.getProgress().money.getDollars(), BUTTON_PROPERTIES_STATIC)
        )), new Vector2i(0, 2));
    }
}

