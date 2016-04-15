package com.yeungeek.monkeyandroid.ui.detail;

import android.text.TextUtils;

import com.yeungeek.monkeyandroid.data.DataManager;
import com.yeungeek.monkeyandroid.data.model.Repo;
import com.yeungeek.monkeyandroid.ui.base.presenter.MvpLceRxPresenter;
import com.yeungeek.monkeyandroid.util.HttpStatus;
import com.yeungeek.mvp.common.MvpPresenter;

import javax.inject.Inject;

import retrofit2.Response;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Created by yeungeek on 2016/4/13.
 */
public class RepoDetailPresenter extends MvpLceRxPresenter<RepoDetailMvpView, String> implements MvpPresenter<RepoDetailMvpView> {
    private final DataManager dataManager;
    private String cssFile = "file:///android_asset/markdown_css_themes/classic.css";

    private Subscriber<Response<Void>> mCheckStar;

    @Inject
    public RepoDetailPresenter(final DataManager dataManager) {
        this.dataManager = dataManager;
    }

    public void getReadme(final String owner, final String repo, final boolean pullToRefresh) {
        Timber.d("### getReadme owner:%s, repo: %s", owner, repo);
        subscribe(dataManager.getReadme(owner, repo, cssFile), pullToRefresh);
    }

    public void checkIfStaring(final Repo repo) {
        Timber.d("### getReadme owner:%s, repo: %s", repo.getOwner().getLogin(), repo.getName());
        dataManager.checkIfStaring(repo.getOwner().getLogin(), repo.getName())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mCheckStar = new Subscriber<Response<Void>>() {
                    @Override
                    public void onCompleted() {
                        if (isViewAttached()) {
                            getView().showContent();
                        }
                        unsubscribe();
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (isViewAttached()) {
                            getView().showError(e, true);
                        }
                        unsubscribe();
                    }

                    @Override
                    public void onNext(Response<Void> response) {
                        if (null != response && response.code() == HttpStatus.HTTP_NO_CONTENT) {
                            getView().checkIfStaring(true);
                        } else {
                            getView().checkIfStaring(false);
                        }
                    }
                });
    }

    public boolean isLogined() {
        return !TextUtils.isEmpty(dataManager.getPreferencesHelper().getAccessToken());
    }

    @Override
    protected void unsubscribe() {
        super.unsubscribe();
        if (null != mCheckStar && mCheckStar.isUnsubscribed()) {
            mCheckStar.unsubscribe();
        }

        mCheckStar = null;
    }

    @Override
    public void detachView(boolean retainInstance) {
        super.detachView(retainInstance);
        unsubscribe();
    }
}