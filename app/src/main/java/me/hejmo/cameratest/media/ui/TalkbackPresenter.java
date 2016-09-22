package me.hejmo.cameratest.media.ui;

/**
 * @author qinglian.zhang
 */

public class TalkbackPresenter implements TalkbackContract.Presenter {

    private TalkbackContract.View view;

    public TalkbackPresenter(TalkbackContract.View view){
        this.view = view;
        view.setPresenter(this);
    }

    @Override
    public void start() {

    }
}
