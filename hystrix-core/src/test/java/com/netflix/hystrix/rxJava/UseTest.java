package com.netflix.hystrix.rxJava;

import com.netflix.hystrix.HystrixCachedObservable;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.ReplaySubject;


/**
 * Created by zhaojunting1 on 2019/2/16
 */
public class UseTest {


    private Logger logger = LoggerFactory.getLogger(UseTest.class);

    /**
     * 变换
     */
    @Test
    public void t1(){
        Observable.just("1","2","3").flatMap(new Func1<String, Observable<String>>() {
            @Override
            public Observable<String> call(String s) {
                return Observable.just(s + "^_^");
            }
        }).subscribe(new Observer<String>() {
            @Override
            public void onCompleted() {
                logger.info("onCompleted running !");
                System.out.println("onCompleted running !");
            }

            @Override
            public void onError(Throwable e) {
                logger.error("onError running !,", e);
                System.out.println("onError running !" + e.getMessage());
            }

            @Override
            public void onNext(String o) {
                logger.info("onNext running: arg : {} ", o);
                System.out.println("onNext running: arg : " + o);
            }
        });
    }


    /**
     * 调度
     */
    @Test
    public void t2(){
        Observable.just(1,2,3,4)
            .doOnSubscribe(new Action0() {
                @Override
                public void call() {
                    System.out.println("Thread[" + Thread.currentThread().getId() + "]: doOnSubscribe call !");
                }
            })
            .subscribeOn(Schedulers.newThread())
            .observeOn(Schedulers.newThread())
            .subscribe(new Subscriber<Integer>() {
                @Override
                public void onCompleted() {
//                    System.out.println("onCompleted running !");
                    System.out.println("Thread[" + Thread.currentThread().getId() + "]: onCompleted running !");
                }

                @Override
                public void onError(Throwable e) {
                    System.out.println("onError running !" + e.getMessage());
                }

                @Override
                public void onNext(Integer integer) {
//                    System.out.println("onNext running: arg : " + integer);
                    System.out.println("Thread[" + Thread.currentThread().getId() + "]: onNext running: arg : " + integer);
                }
            });

        try {
            Thread.sleep(1000l);
            System.out.println("Thread[" + Thread.currentThread().getId() + "]: Over !");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 3.RxJava: RelaySubject
     * 事件重放
     * https://mcxiaoke.gitbooks.io/rxdocs/content/Subject.html
     */
    @Test
    public void t3(){
        Observable<Integer> source = Observable.create(new Observable.OnSubscribe<Integer>() {
            @Override
            public void call(Subscriber<? super Integer> subscriber) {
                System.out.println(" OnSubscribe running !");
                subscriber.onNext(1);
                subscriber.onCompleted();
            }
        });

        source.subscribe(new NamedSubscriber<Integer>("source") {
            @Override
            public void onCompleted() {
                System.out.println("source onCompleted running !");
            }

            @Override
            public void onError(Throwable e) {
                System.out.println("source onError running , " + e.getMessage());
            }

            @Override
            public void onNext(Integer integer) {
                System.out.println("source onNext running: arg : " + integer);
            }
        });

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }

        /** 直接通过原Observable订阅会重新触发事件发射（比如网络请求）*/
        source.subscribe(new NamedSubscriber<Integer>("source1") {
            @Override
            public void onCompleted() {
                System.out.println("source1 onCompleted running !");
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Integer integer) {
                System.out.println("source1 onNext running: arg : " + integer);
            }
        });


        ReplaySubject<Integer> subject = ReplaySubject.create();
        /** 通过ReplaySubject进行订阅，只有在首次订阅时，触发Observable事件发射 */
        source.subscribe(subject);
        System.out.println("source.subscribe(subject) running !");

        /** 后面通过subject.asObservable();都只是对原事件序列重放 */
        //模拟第一个请求，走缓存
        Observable<Integer> asObservable = subject.asObservable();


        asObservable.subscribe(new NamedSubscriber<Integer>("asObservable") {
            @Override
            public void onCompleted() {
                System.out.println("asObservable onCompleted running !");
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Integer integer) {
                System.out.println("asObservable onNext running: arg : " + integer);
            }
        });

        //模拟第二个请求，走缓存
        Observable<Integer> asObservable1 = subject.asObservable();

        asObservable1.subscribe(new NamedSubscriber<Integer>("asObservable1")  {
            @Override
            public void onCompleted() {
                System.out.println("asObservable1 onCompleted running !");
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Integer integer) {
                System.out.println("asObservable1 onNext running: arg : " + integer);
            }
        });


        asObservable.subscribe(new NamedSubscriber<Integer>("asObservable2") {
            @Override
            public void onCompleted() {
                System.out.println("asObservable2 onCompleted running !");
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Integer integer) {
                System.out.println("asObservable2 onNext running: arg : " + integer);
            }
        });


        ReplaySubject<Integer> subject1 = ReplaySubject.create();
        /** 通过ReplaySubject进行订阅，只有在首次订阅时，触发Observable事件发射,换一个ReplaySubject订阅再次触发原来Observable事件发射 */
        source.subscribe(subject1);
        System.out.println("source.subscribe(subject1) running !");
        subject1.asObservable().subscribe(new NamedSubscriber<Integer>("subject1-asObservable") {
            @Override
            public void onCompleted() {
                System.out.println("subject1.asObservable onCompleted running !");
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Integer integer) {
                System.out.println("subject1.asObservable onNext running: arg : " + integer);
            }
        });
    }

    /**
     * 多线程测试请求缓存是否会触发多次事件发射
     */
    @Test
    public void t4(){
        final Observable<Integer> source = Observable.create(new Observable.OnSubscribe<Integer>() {
            @Override
            public void call(Subscriber<? super Integer> subscriber) {
                System.out.println("source OnSubscribe running !");
                subscriber.onNext(1);
                subscriber.onCompleted();
            }
        });

        for(int i = 0; i < 10; i++) {
            new Thread(
                    new Runnable() {
                        @Override
                        public void run() {
                            HystrixCachedObservable<Integer> toCache = HystrixCachedObservable.from(source);
                        }
                    }
            ).start();
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }

    }

    private int i = 0;
    /**
     * 测试RxJava创建操作之---Defer：
     */
    @Test
    public void t5(){

        Observable<Integer> defer = Observable.defer(new Func0<Observable<Integer>>(){
            @Override
            public Observable<Integer> call() {
                System.out.println("Func0 running i = " + i);
                return Observable.create(new Observable.OnSubscribe<Integer>() {
                    @Override
                    public void call(Subscriber<? super Integer> subscriber) {
                        subscriber.onNext(1);
                        subscriber.onCompleted();
                    }
                });
            }
        });

        defer.subscribe(new Subscriber<Integer>() {
            @Override
            public void onCompleted() {
                System.out.println("defer.subscribe1 onCompleted running !");
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Integer integer) {
                System.out.println("defer.subscribe1 onNext running: arg : " + integer);
            }
        });

        defer.subscribe(new Subscriber<Integer>() {
            @Override
            public void onCompleted() {
                System.out.println("defer.subscribe2 onCompleted running !");
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Integer integer) {
                System.out.println("defer.subscribe2 onNext running: arg : " + integer);
            }
        });

    }















    abstract class NamedSubscriber<T> extends Subscriber<T>{
        private String name;

        public NamedSubscriber(String name) {
            super();
            this.name = name;
        }
    }
}
