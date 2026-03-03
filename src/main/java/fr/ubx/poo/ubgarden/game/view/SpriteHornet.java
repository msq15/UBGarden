package fr.ubx.poo.ubgarden.game.view;

import fr.ubx.poo.ubgarden.game.Direction;
import fr.ubx.poo.ubgarden.game.go.personage.Hornet;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;

public class SpriteHornet extends Sprite {

    public SpriteHornet(Pane layer, Hornet hornet) {
        super(layer, null, hornet);
        updateImage();
    }

    @Override
    public void updateImage() {
        Hornet hornet = (Hornet) getGameObject();
        if (hornet == null || hornet.isDeleted()) return;
        Image image = getImage(hornet.getDirection());
        setImage(image);
    }

    private Image getImage(Direction direction) {
        return ImageResourceFactory.getInstance().getHornet(direction);
    }
}