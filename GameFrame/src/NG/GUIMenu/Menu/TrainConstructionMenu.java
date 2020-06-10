package NG.GUIMenu.Menu;

import NG.Core.Game;
import NG.Entities.Locomotive;
import NG.Entities.Train;
import NG.Entities.Wagon;
import NG.GUIMenu.Components.SButton;
import NG.GUIMenu.Components.SContainer;
import NG.GUIMenu.Components.SFrame;
import NG.GUIMenu.Components.SPanel;
import NG.Tracks.TrackPiece;
import org.joml.Vector2i;

import java.util.List;

/**
 * @author Geert van Ieperen created on 21-5-2020.
 */
public class TrainConstructionMenu extends SFrame {
    public TrainConstructionMenu(Game game, TrackPiece place) {
        super("New Train");

        double gameTime = game.timer().getGameTime();
        Train construction = new Train(game, gameTime, place);
        game.state().addEntity(construction);

        List<Locomotive.Properties> locomotiveTypes = game.objectTypes().locomotiveTypes;
        List<Wagon.Properties> wagonTypes = game.objectTypes().wagonTypes;

        SPanel elementsPanel = new SPanel(1, locomotiveTypes.size() + wagonTypes.size());
        int yIndex = 0;

        for (Locomotive.Properties loco : locomotiveTypes) {
            elementsPanel.add(new SButton(
                            "Add " + loco.toString(),
                            () -> construction.addElement(new Locomotive(loco))),
                    new Vector2i(0, yIndex++)
            );
        }

        for (Wagon.Properties wagon : wagonTypes) {
            elementsPanel.add(new SButton(
                            "Add " + wagon.toString(),
                            () -> construction.addElement(new Wagon(wagon))),
                    new Vector2i(0, yIndex++)
            );
        }

        SButton removeButton = new SButton("Remove Last", construction::removeLastElement);

        setMainPanel(SContainer.column(
                elementsPanel, removeButton
        ));
    }
}
