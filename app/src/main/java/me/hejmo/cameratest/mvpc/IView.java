package me.hejmo.cameratest.mvpc;

/**
 * Base interface od View
 */
public interface IView<T extends IPresenter> {

    void setPresenter(T presenter);

}
