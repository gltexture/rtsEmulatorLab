import javax.swing.*;
import java.awt.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    public Main() throws InterruptedException {
        this.initUI();
    }

    private void initUI() throws InterruptedException {
        JFrame frame = new JFrame("Emulation Power Control System");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(800, 560);
        frame.setLayout(new BorderLayout());
        frame.setResizable(false);

        JSlider sliderCurrent = new JSlider(1900, 2100, 2000);
        sliderCurrent.setMajorTickSpacing(25);
        sliderCurrent.setPaintTicks(true);
        sliderCurrent.setPaintLabels(true);

        JLabel SECONDS = new JLabel();
        SECONDS.setFont(new Font("Arial", Font.BOLD, 18));

        JLabel sensorA = new JLabel();
        sensorA.setFont(new Font("Arial", Font.BOLD, 16));

        JLabel ADCVoltage = new JLabel();
        ADCVoltage.setFont(new Font("Arial", Font.BOLD, 16));

        JLabel ADCCode = new JLabel();
        ADCCode.setFont(new Font("Arial", Font.BOLD, 16));

        JLabel DACCodeEntry = new JLabel();
        DACCodeEntry.setFont(new Font("Arial", Font.BOLD, 16));

        JLabel DACOutput = new JLabel();
        DACOutput.setFont(new Font("Arial", Font.BOLD, 16));

        JLabel LAMP_PORT_OUTPUT = new JLabel();
        LAMP_PORT_OUTPUT.setFont(new Font("Arial", Font.BOLD, 16));

        JLabel WARN_A_LAMP = new JLabel("● Unacceptable increase");
        WARN_A_LAMP.setFont(new Font("Arial", Font.BOLD, 24));
        WARN_A_LAMP.setForeground(Color.BLACK);

        JLabel WARN_B_LAMP = new JLabel("● Unacceptable downgrade");
        WARN_B_LAMP.setFont(new Font("Arial", Font.BOLD, 24));
        WARN_B_LAMP.setForeground(Color.BLACK);

        JLabel CRIT_A_LAMP = new JLabel("● Critical increase");
        CRIT_A_LAMP.setFont(new Font("Arial", Font.BOLD, 24));
        CRIT_A_LAMP.setForeground(Color.BLACK);

        JLabel CRIT_B_LAMP = new JLabel("● Critical downgrade");
        CRIT_B_LAMP.setFont(new Font("Arial", Font.BOLD, 24));
        CRIT_B_LAMP.setForeground(Color.BLACK);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createTitledBorder("Sensor Controller"));
        topPanel.add(sliderCurrent, BorderLayout.NORTH);


        JPanel panelCircuityData = new JPanel();
        panelCircuityData.setLayout(new BoxLayout(panelCircuityData, BoxLayout.Y_AXIS));
        panelCircuityData.setBorder(BorderFactory.createTitledBorder("Circuity"));
        panelCircuityData.add(Box.createVerticalStrut(5));
        panelCircuityData.add(SECONDS);
        panelCircuityData.add(Box.createVerticalStrut(10));
        panelCircuityData.add(sensorA);
        panelCircuityData.add(ADCVoltage);
        panelCircuityData.add(ADCCode);
        panelCircuityData.add(Box.createVerticalStrut(5));
        panelCircuityData.add(DACCodeEntry);
        panelCircuityData.add(DACOutput);
        panelCircuityData.add(Box.createVerticalStrut(5));
        panelCircuityData.add(LAMP_PORT_OUTPUT);

        JPanel lamps = new JPanel();
        lamps.setLayout(new BoxLayout(lamps, BoxLayout.Y_AXIS));
        lamps.setBorder(BorderFactory.createTitledBorder("LAMPS"));
        lamps.add(Box.createVerticalStrut(15));
        lamps.add(WARN_A_LAMP);
        lamps.add(WARN_B_LAMP);
        lamps.add(CRIT_A_LAMP);
        lamps.add(CRIT_B_LAMP);

        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(panelCircuityData, BorderLayout.CENTER);
        frame.add(lamps, BorderLayout.SOUTH);
        frame.setVisible(true);

        final float uConvC = 0.0075f;

        final int alarmMax = 55;
        final int alarmMin = 0;
        final float alarmMaxTime = 50.0f;
        final float alarmTimeToStop = 45.0f;
        final float alarmLinearTime = 7.3f;

        // t_step = 7.3 // 55 = 0.1327
        // freq = 2.5 ghz = 2.5 * 10^9
        // ticks_per_step = t_step * freq = 2.5e9 * 0.1327 = 331750000 ticks

        // delay_ms = t_step * 1000 = 132,7 ms
        // PRC FREQ = 2.5 ghz

        AtomicInteger secondsTime = new AtomicInteger();
        AtomicInteger milliSecondsTime = new AtomicInteger();
        AtomicBoolean START = new AtomicBoolean(false);

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            while (true) {
                if (!START.get()) {
                    continue;
                }
                if (milliSecondsTime.incrementAndGet() >= 10) {
                    secondsTime.incrementAndGet();
                    milliSecondsTime.set(0);
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        int oldMs = 0;
        int alarmCode = 0;

        final int warn_above = 32850;
        final int crit_above = 32883;
        final int warn_below = 32686;
        final int crit_below = 32653;

        while (true) {
            final int A = sliderCurrent.getValue();
            final float U = (A * uConvC);
            final int adcCode = (int) (Math.round(((U) / (30.0f)) * Math.pow(2, 16)));

            final int sec = secondsTime.get();
            final int milliSec = milliSecondsTime.get();

            boolean WARN_ABOVE = adcCode >= warn_above;
            boolean WARN_BELOW = adcCode <= warn_below;
            boolean CRIT_ABOVE = adcCode >= crit_above;
            boolean CRIT_BELOW = adcCode <= crit_below;

            int lampsCode = 0;
            if (CRIT_ABOVE) {
                lampsCode |= 1 << 3;
                CRIT_A_LAMP.setForeground(Color.RED);
                WARN_A_LAMP.setForeground(Color.BLACK);
            } else {
                if (WARN_ABOVE) {
                    lampsCode |= 1 << 1;
                    WARN_A_LAMP.setForeground(Color.ORANGE);
                } else {
                    WARN_A_LAMP.setForeground(Color.BLACK);
                }
                CRIT_A_LAMP.setForeground(Color.BLACK);
            }

            if (CRIT_BELOW) {
                lampsCode |= 1 << 2;
                CRIT_B_LAMP.setForeground(Color.RED);
                WARN_B_LAMP.setForeground(Color.BLACK);
            } else {
                if (WARN_BELOW) {
                    lampsCode |= 1;
                    WARN_B_LAMP.setForeground(Color.ORANGE);
                } else {
                    WARN_B_LAMP.setForeground(Color.BLACK);
                }
                CRIT_B_LAMP.setForeground(Color.BLACK);
            }

            if ((CRIT_ABOVE || CRIT_BELOW) && sec < 50) {
                if (sec < 45) {
                    if (alarmCode < alarmMax) {
                        alarmCode += 1;
                        Thread.sleep(132);
                    }
                } else {
                    alarmCode = 0;
                }
                START.set(true);
            } else {
                secondsTime.set(0);
                milliSecondsTime.set(0);
                alarmCode = 0;
                START.set(false);
            }

            sensorA.setText("Sensor, A=" + A);
            ADCVoltage.setText("ADC, Voltage =" + U + " V");
            ADCCode.setText("ADC -> (IN-PORT 300h)=" + adcCode);

            final float alarmVoltage = ((float) alarmCode / alarmMax) * 12.0f;

            LAMP_PORT_OUTPUT.setText("OUT-PORT 301h -> LAMPS: " + String.format("%4s", Integer.toBinaryString(lampsCode)).replace(' ', '0'));
            DACCodeEntry.setText("OUT-PORT 502h -> ALARM DAC: " + alarmCode);
            DACOutput.setText("DAC Voltage Output: " + alarmVoltage + " V");
            SECONDS.setText("Alarm Activity (Sec): " + (sec + "," + milliSec));

            if (oldMs != milliSec && sec <= 8) {
                System.out.println("SEC " + (sec + "," + milliSec) + ". ALARM_VOLT = " + alarmVoltage);
            }
            oldMs = milliSec;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        new Main();
    }
}