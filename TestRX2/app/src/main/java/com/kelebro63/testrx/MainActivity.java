package com.kelebro63.testrx;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.observers.TestSubscriber;

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
        testSchedulersTemplate(stringObservable -> stringObservable);
    }

    @OnClick(R.id.btn_clear_operation)
    public void clearOperation() {
        _logs.clear();
        _adapter.clear();
    }

    private String _log(String logMsg) {

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
        return logMsg;
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


    private void testSchedulersTemplate(Observable.Transformer transformer) {
        Observable<String> obs = Observable
                .create(subscriber -> {
                    logThread("Inside observable");
                    subscriber.onNext("Hello from observable");
                    subscriber.onCompleted();
                })
                .doOnNext(s -> logThread("Before transform"))
                .compose(transformer)
                .doOnNext(s -> logThread("After transform"));
        TestSubscriber<String> subscriber = new TestSubscriber<>(new Subscriber<String>() {
            @Override
            public void onCompleted() {
                logThread("In onComplete");
            }

            @Override
            public void onError(Throwable e) {}

            @Override
            public void onNext(String o) {
                logThread("In onNext");
            }
        });
        obs.subscribe(subscriber);
        subscriber.awaitTerminalEvent();
    }

    public void logThread(String message) {
        //Logger.d(message + " : " + Thread.currentThread().getName());
        _log(message);
    }

}
