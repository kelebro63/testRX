package com.kelebro63.testrx;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.fernandocejas.frodo.annotation.RxLogObservable;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity {


    @Bind(R.id.progress_operation_running)
    ProgressBar _progress;
    @Bind(R.id.list_threading_log)
    ListView _logsList;

    private LogAdapter _adapter;
    private List<String> _logs;
    private Subscription _subscription;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        _setupLogger();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (_subscription != null) {
            _subscription.unsubscribe();
        }
    }

    @OnClick(R.id.btn_start_operation)
    public void startLongOperation() {

        _progress.setVisibility(View.VISIBLE);
        _log("Button Clicked");

        _subscription = _getObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())

                //**********************1********************
//                .subscribe(new Action1<List<Integer>>() {
//                               @Override
//                               public void call(List<Integer> integers) {
//                                    String test = "";
//                               }
//                           }, new Action1<Throwable>() {
//                               @Override
//                               public void call(Throwable throwable) {
//                                   String test = "";
//                               }
//                           });


                //**************2**********************
//                .subscribe(
//                        System.out::println,
//                        error -> Toast.makeText(this, error.toString(), Toast.LENGTH_LONG).show()
//                );


        //*********************3***********************
                .subscribe(_getObserver());
    }

    @OnClick(R.id.btn_clear_operation)
    public void clearOperation() {
        _logs.clear();
        _adapter.clear();
    }

    @RxLogObservable
    private Observable<List<Integer>> _getObservable() {

        return Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(final Subscriber<? super String> observer) {
                observer.onNext("1");
                observer.onNext("2");
                observer.onNext("3");
                observer.onNext("4");
                observer.onError(new Throwable());
                observer.onCompleted();
            }
        })
//                .onErrorReturn(throwable -> {
//                    return "100";
//                })
                .map(new Func1<String, Integer>() {
                    @Override
                    public Integer call(String s) {
                        _log("Within Observable");
                        _doSomeLongOperation_thatBlocksCurrentThread();
                        return Integer.valueOf(s);
                    }
                })
//                .onErrorReturn(throwable -> {
//                    return 100;
//                })
                .flatMap(new Func1<Integer, Observable<Integer>>() {
                    @Override
                    public Observable<Integer> call(Integer i) {
                        ArrayList<Integer> list = new ArrayList<Integer>();
                        for (int j = 0; j < i; ++j) {
                            list.add(j);
                        }
                        return Observable.from(list);
                    }
                })
             //   .onErrorResumeNext(throwable -> {return Observable.just(1);})
                .toList()
                .first();

    }


    public Observer<List<Integer>> _getObserver() {
        return new Observer<List<Integer>>() {

            @Override
            public void onCompleted() {
                _log("On complete");
                _progress.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onError(Throwable e) {
                Timber.e(e, "Error in RxJava Demo concurrency");
                _log(String.format("Boo! Error %s", e.getMessage()));
                _progress.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onNext(List<Integer> i) {
                //_log(String.format("onNext with return value \"%b\"", bool));
                _log("onNext with return value" + i);
            }
        };
    }


    private void _doSomeLongOperation_thatBlocksCurrentThread() {
        _log("performing long operation");

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Timber.d("Operation was interrupted");
        }
    }

    private void _log(String logMsg) {

        if (_isCurrentlyOnMainThread()) {
            _logs.add(0, logMsg + " (main thread) ");
            _adapter.clear();
            _adapter.addAll(_logs);
        } else {
            _logs.add(0, logMsg + " (NOT main thread) ");

            // You can only do below stuff on main thread.
            new Handler(Looper.getMainLooper()).post(new Runnable() {

                @Override
                public void run() {
                    _adapter.clear();
                    _adapter.addAll(_logs);
                }
            });
        }
    }

    private void _setupLogger() {
        _logs = new ArrayList<String>();
        _adapter = new LogAdapter(this, new ArrayList<String>());
        _logsList.setAdapter(_adapter);
    }

    private boolean _isCurrentlyOnMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    private class LogAdapter
            extends ArrayAdapter<String> {

        public LogAdapter(Context context, List<String> logs) {
            super(context, R.layout.item_log, R.id.item_log, logs);
        }
    }


}
