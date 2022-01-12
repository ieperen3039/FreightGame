package NG.Menu.InGame;

import NG.Core.FreightGame;
import NG.Core.Game;
import NG.Core.ModLoader;
import NG.GUIMenu.Components.*;
import NG.GUIMenu.Rendering.NGFonts;
import NG.GUIMenu.Rendering.SFrameLookAndFeel;
import NG.GUIMenu.SComponentProperties;
import NG.Menu.InGame.Build.BuildMenu;
import NG.Menu.InGame.Overviews.TrainOverview;
import NG.Network.NetworkNode;
import NG.Network.RailNode;
import NG.AssetHandling.Asset;
import NG.Tools.Directory;
import NG.Tools.Logger;
import NG.Tools.Vectors;
import NG.Tracks.TrackPiece;

import java.util.stream.Stream;

import static NG.Menu.Main.MainMenu.BUTTON_PROPERTIES_STRETCH;
import static NG.Menu.Main.MainMenu.TEXT_PROPERTIES;

/**
 * @author Geert van Ieperen created on 2-9-2020.
 */
public class FreightGameUI extends SDecorator {
    private static final SComponentProperties MONEY_TEXT_PROPERTIES = new SComponentProperties(
            0, 100, true, false, NGFonts.TextType.FANCY, SFrameLookAndFeel.Alignment.CENTER_MIDDLE
    );

    private final SComponentArea mainArea;

    public FreightGameUI(Game game, ModLoader modLoader) {
        setContents(SContainer.column(
                mainArea = new SComponentArea(0, 0).setGrowthPolicy(true, true),
                SContainer.row(
                        // money element
                        new SPanel(
                                new SActiveTextArea(() -> "$" + game.playerStatus().money.getDollars(), MONEY_TEXT_PROPERTIES)
                        ),
                        new SPanel(SContainer.column(
                                getBottomButtonRow(game, modLoader),
                                new SFiller()
                        )),
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
                                SContainer.column(
                                        new SPanel(new STextArea("Build Menu", TEXT_PROPERTIES)),
                                        new BuildMenu(game, mainArea::hide).setGrowthPolicy(false, true)
                                ), new SFiller()
                        ))
                ),
                new SButton(
                        "Overviews",
                        () -> mainArea.show(new STabPanel.Builder()
                                .add("Trains", new TrainOverview(game))
                                .get()
                        )
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
                new SButton("Dump Assets", () -> Asset.forEach(Logger.DEBUG::print)),
                new SButton("Save Game", () -> modLoader.saveGame(FreightGame.SAVE_FILE), BUTTON_PROPERTIES_STRETCH),
                new SButton("Exit", () -> {
                    modLoader.saveGame(FreightGame.SAVE_FILE);
                    modLoader.stopGame();
                }, BUTTON_PROPERTIES_STRETCH)
        ));
    }

}

