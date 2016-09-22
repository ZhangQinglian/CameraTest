package me.hejmo.cameratest.media.ui;

import me.hejmo.cameratest.mvpc.IContract;
import me.hejmo.cameratest.mvpc.IPresenter;
import me.hejmo.cameratest.mvpc.IView;

/**
 * Created by scott on 2016/9/22.
 */

public class TalkbackContract implements IContract {

    interface Presenter extends IPresenter{

    }

    interface View extends IView<Presenter>{

    }
}
