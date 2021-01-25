package io.reactivex.rxjava3.android.samples;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Function;

/**
 * Description:
 *
 * @author Li Jianying
 * @version 1.0
 * @since 2021/1/25
 */
public class RxTest {

    private final static String tag = "rx_run";


    public static void main(String[] args) {
        testRx();
    }


    private static void testRx() {

        System.out.println("rx_run：" + "============begin   create Observable ================\n");

        ObservableOnSubscribe<String> source = new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<String> emitter) throws Throwable {
                System.out.println("rx_run：" + "source invoke subscribe：" + emitter.getClass().getSimpleName());
                System.out.println("rx_run：" + "emitter invoke onNext =======");

                emitter.onNext("1131231232");
                System.out.println("rx_run：" + "emitter invoke onComplete =======");
                emitter.onComplete();
            }
        };

        System.out.println("rx_run：" + "create source object："+source.getClass().getName());

        Observable<String> createObservable = Observable.create(source);

        Function<String, Integer> mapFunction = new Function<String, Integer>() {
            @Override
            public Integer apply(String s) throws Throwable {
                System.out.println("rx_run：" + "mapFunction invoke convert string to int  =======");
                return Integer.parseInt(s);
            }
        };

        Observable<Integer> mapObservable = createObservable.map(mapFunction);

        Observer<Integer> observer = new Observer<Integer>() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {
                System.out.println("rx_run：" + "observer invoke onSubscribe method  =======");
            }

            @Override
            public void onNext(@NonNull Integer integer) {
                System.out.println("rx_run：" + "observer invoke onNext method  =======" + integer);
            }

            @Override
            public void onError(@NonNull Throwable e) {
                System.out.println("rx_run：" + "observer invoke onError method  =======" + e.getMessage());
            }

            @Override
            public void onComplete() {
                System.out.println("rx_run：" + "observer invoke onComplete method  =======");
            }
        };

        System.out.println("\n\n============Observable begin   subscribe================\n");
        System.out.println("rx_run：" + "create observer object："+observer.getClass().getName());

        mapObservable.subscribe(observer);
    }

}
