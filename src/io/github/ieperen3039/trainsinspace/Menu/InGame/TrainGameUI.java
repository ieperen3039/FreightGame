package io.github.ieperen3039.trainsinspace.Menu.InGame;

import io.github.ieperen3039.trainsinspace.Core.TrainGame;
import io.github.ieperen3039.trainsinspace.Core.Game;
import io.github.ieperen3039.trainsinspace.Core.ModLoader;
import io.github.ieperen3039.trainsinspace.GUIMenu.Components.*;
import io.github.ieperen3039.trainsinspace.GUIMenu.Rendering.NGFonts;
import io.github.ieperen3039.trainsinspace.GUIMenu.Rendering.SFrameLookAndFeel;
import io.github.ieperen3039.trainsinspace.GUIMenu.SComponentProperties;
import io.github.ieperen3039.trainsinspace.Menu.InGame.Build.BuildMenu;
import io.github.ieperen3039.trainsinspace.Menu.InGame.Overviews.TrainOverview;
import io.github.ieperen3039.trainsinspace.Network.NetworkNode;
import io.github.ieperen3039.trainsinspace.Network.RailNode;
import io.github.ieperen3039.trainsinspace.AssetHandling.Asset;
import io.github.ieperen3039.trainsinspace.Tools.Directory;
import io.github.ieperen3039.trainsinspace.Tools.Logger;
import io.github.ieperen3039.trainsinspace.Tools.Vectors;
import io.github.ieperen3039.trainsinspace.Tracks.TrackPiece;

import java.util.stream.Stream;

import static io.github.ieperen3039.trainsinspace.Menu.Main.MainMenu.*;

/**
 * @author Geert van Ieperen created on 2-9-2020.
 */
public class TrainGameUI extends SDecorator {
    private static final SComponentProperties MONEY_TEXT_PROPERTIES = new SComponentProperties(
            0, 100, true, false, NGFonts.TextType.FANCY, SFrameLookAndFeel.Alignment.CENTER_MIDDLE
    );

    private final SComponentArea mainArea;

    public TrainGameUI(Game game, ModLoader modLoader) {
        setContents(SContainer.column(
                mainArea = new SComponentArea(0, 0).setGrowthPolicy(true, true),
                SContainer.row(
                        // money element
                        new SPanel(
                                new SActiveTextArea(() -> "$" + game.playerStatus().money.getDollars(), MONEY_TEXT_PROPERTIES)
                        ),
                        SPanel.column(
                                getBottomButtonRow(game, modLoader),
                                new SFiller()
                        ),
                        new SPanel(new SFiller())
                ).setGrowthPolicy(true, false)
        ));
        Logger.printOnline(() -> Vectors.toString(mainArea.getSize()));
    }

    private SContainer getBottomButtonRow(Game game, ModLoader modLoader) {
        return SContainer.row(
                new SButton(
                        "Build Object",
                        () -> mainArea.show(SContainer.row(
                                SPanel.column(
                                        new STextArea("Build Menu", TEXT_PROPERTIES),
                                        new BuildMenu(game, mainArea::hide).setGrowthPolicy(false, true)
                                ),
                                new SFiller()
                        ))
                ),
                new SButton(
                        "Overviews",
                        () -> mainArea.show(SContainer.column(
                                new STabPanel.Builder()
                                        .add("Trains", new TrainOverview(game))
                                        .get(TEXT_PROPERTIES),
                                SPanel.row(
                                        new SFiller(),
                                        new SButton("Close", mainArea::hide, BUTTON_PROPERTIES_STATIC)
                                )
                        ))
                ),
                new SButton(
                        "Options",
                        () -> game.gui().addFrameCenter(
                                getOptionsMenu(game, modLoader),
                                game.window()
                        )
                )
        );
    }

    private SFrame getOptionsMenu(Game game, ModLoader modLoader) {
        return new SFrame("Options", SContainer.column(
                new SToggleButton(
                        "Show CollisionBox", BUTTON_PROPERTIES_STRETCH, game.settings().RENDER_COLLISION_BOX
                ).addStateChangeListener(active -> game.settings().RENDER_COLLISION_BOX = active),

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
                                ),
                        BUTTON_PROPERTIES_STRETCH
                ),

                new SButton("Check Network", // checks the NetworkNodes of all track pieces
                        () -> game.state().entities().stream()
                                .filter(e -> e instanceof TrackPiece)
                                .map(e -> (TrackPiece) e)
                                .filter(e -> !e.isDespawnedAt(game.timer().getGameTime()))
                                .peek(t -> {
                                    if (!t.isValid()) {
                                        throw new IllegalStateException(String.valueOf(t));
                                    }
                                })
                                .flatMap(t -> Stream.of(t.getStartNode(), t.getEndNode()))
                                .distinct()
                                .map(RailNode::getNetworkNode)
                                .forEach(NetworkNode::check),
                        BUTTON_PROPERTIES_STRETCH
                ),

                new SButton("Dump light map",
                        () -> game.executeOnRenderThread(
                                () -> game.lights().dumpShadowMap(Directory.screenshots)
                        ),
                        BUTTON_PROPERTIES_STRETCH
                ),
                new SButton("Reload Assets", () -> game.executeOnRenderThread(Asset::dropAll)),
                new SButton("Dump Assets", () -> Asset.forEachActive(Logger.DEBUG::print)),
                new SButton("Save Game", () -> modLoader.saveGame(TrainGame.SAVE_FILE), BUTTON_PROPERTIES_STRETCH),
                new SButton("Exit", () -> {
                    modLoader.saveGame(TrainGame.SAVE_FILE);
                    modLoader.stopGame();
                }, BUTTON_PROPERTIES_STRETCH)
        ));
    }

}

