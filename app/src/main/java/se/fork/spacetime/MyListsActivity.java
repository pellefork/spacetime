package se.fork.spacetime;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import pl.pawelkleczkowski.customgauge.CustomGauge;

public class MyListsActivity extends AppCompatActivity {

    private CustomGauge gauge;
    private Button plusButton;
    private Button minusButton;
    private TextView valueView;
    private int value;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_lists);
        gauge = findViewById(R.id.gauge1);
        plusButton = findViewById(R.id.increment);
        minusButton = findViewById(R.id.decrement);
        valueView = findViewById(R.id.value);
        value = 0;

        plusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread() {
                    public void run() {
                        int i=value;
                        for (;i<value + 10 ;i++) {
                            try {
                                final int finalI = i;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        gauge.setValue(finalI);
                                        valueView.setText(Integer.toString(finalI) + "%");
                                    }
                                });
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        value = i;
                    }
                }.start();

            }
        });

        minusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread() {
                    public void run() {
                        int i=value;
                        for (;i>value - 10 ;i--) {
                            try {
                                final int finalI = i;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        gauge.setValue(finalI);
                                        valueView.setText(Integer.toString(finalI) + "%");
                                    }
                                });
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        value = i;
                    }
                }.start();

            }
        });

    }
}
