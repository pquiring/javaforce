package javaforce.utils;

import java.util.*;

import javaforce.*;
import javaforce.awt.*;
import javaforce.cl.*;

/** CPU Hogger
 *
 * Utility that consumes 100% CPU for system stress testing.
 *
 * @author User
 */
public class CPUHogger extends javax.swing.JFrame {

  public static boolean debug = true;

  /**
   * Creates new form CPUHogger
   */
  public CPUHogger() {
    initComponents();
    JFAWT.centerWindow(this);
    selection.add(select_cpu);
    selection.add(select_gpu);
  }

  /**
   * This method is called from within the constructor to initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is always
   * regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {

    selection = new javax.swing.ButtonGroup();
    jLabel1 = new javax.swing.JLabel();
    threads = new javax.swing.JSpinner();
    start = new javax.swing.JButton();
    status = new javax.swing.JLabel();
    select_cpu = new javax.swing.JRadioButton();
    select_gpu = new javax.swing.JRadioButton();
    jLabel2 = new javax.swing.JLabel();
    matrix_size = new javax.swing.JTextField();

    setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
    setTitle("CPU Hogger");

    jLabel1.setText("# of Threads");

    threads.setModel(new javax.swing.SpinnerNumberModel(1, 1, 8, 1));

    start.setText("Start");
    start.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        startActionPerformed(evt);
      }
    });

    status.setText("Status : Idle");

    select_cpu.setSelected(true);
    select_cpu.setText("CPU");

    select_gpu.setText("GPU");

    jLabel2.setText("Matrix Size");

    matrix_size.setText("256");

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
    getContentPane().setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addComponent(start, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
          .addComponent(status, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
          .addGroup(layout.createSequentialGroup()
            .addComponent(jLabel1)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addComponent(threads, javax.swing.GroupLayout.DEFAULT_SIZE, 172, Short.MAX_VALUE))
          .addGroup(layout.createSequentialGroup()
            .addComponent(select_cpu)
            .addGap(0, 0, Short.MAX_VALUE))
          .addGroup(layout.createSequentialGroup()
            .addComponent(select_gpu)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jLabel2)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(matrix_size)))
        .addContainerGap())
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(jLabel1)
          .addComponent(threads, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(select_cpu)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(select_gpu)
          .addComponent(jLabel2)
          .addComponent(matrix_size, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        .addComponent(start)
        .addGap(18, 18, 18)
        .addComponent(status)
        .addContainerGap())
    );

    pack();
  }// </editor-fold>//GEN-END:initComponents

  private void startActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startActionPerformed
    startTest();
  }//GEN-LAST:event_startActionPerformed

  /**
   * @param args the command line arguments
   */
  public static void main(String[] args) {
    /* Create and display the form */
    java.awt.EventQueue.invokeLater(new Runnable() {
      public void run() {
        new CPUHogger().setVisible(true);
      }
    });
  }

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JLabel jLabel1;
  private javax.swing.JLabel jLabel2;
  private javax.swing.JTextField matrix_size;
  private javax.swing.JRadioButton select_cpu;
  private javax.swing.JRadioButton select_gpu;
  private javax.swing.ButtonGroup selection;
  private javax.swing.JButton start;
  private javax.swing.JLabel status;
  private javax.swing.JSpinner threads;
  // End of variables declaration//GEN-END:variables


  private volatile boolean running = false;

  private void startTest() {
    if (running) {
      running = false;
      start.setText("Start");
      status.setText("Status : Idle");
    } else {
      running = true;
      if (select_cpu.isSelected()) {
        startCPUThreads();
      }
      if (select_gpu.isSelected()) {
        startGPUThreads();
      }
      start.setText("stop");
      int cnt = (Integer)threads.getValue();
      status.setText("Status : " + cnt + " threads running");
    }
  }

  private void startCPUThreads() {
    int cnt = (Integer)threads.getValue();
    for(int a=0;a<cnt;a++) {
      CPU hogger = new CPU(a);
      hogger.start();
    }
  }

  private void startGPUThreads() {
    int cnt = (Integer)threads.getValue();
    int mat_size = Integer.valueOf(matrix_size.getText());
    for(int a=0;a<cnt;a++) {
      GPU hogger = new GPU(a, mat_size);
      hogger.start();
    }
  }

  private class CPU extends Thread {
    public int id;
    public int c = 0;
    public CPU(int id) {
      this.id = id;
    }
    public void run() {
      Random r = new Random();
      while (running) {
        float f = r.nextFloat();
        double d = Math.sin(f);
        int z = convert(d);
        c += z;
      }
    }
    private int convert(double d) {
      return (int)(d + 1);
    }
  }

  private static Object cl_lock = new Object();
  private static boolean cl_init;
  private static boolean cl_fail;

  private class GPU extends Thread {
    public int id;
    public GPU(int id, int matrix_size) {
      this.id = id;
      SIZE = matrix_size;
      SIZE_SIZE = SIZE * SIZE;
    }
    public void run() {
      synchronized (cl_lock) {
        if (cl_fail) return;
        if (!cl_init) {
          if (!CL.init()) {
            cl_fail = true;
            if (running) {
              startTest();
            }
            JFAWT.showError("Error", "OpenCL init failed");
            return;
          }
          cl_init = true;
        }
      }
      load();
      while (running) {
        test();
      }
      unload();
    }
    private Compute cmp;
    private static int SIZE;
    private static int SIZE_SIZE;
    private float[] in1;
    private float[] in2;
    private float[] out;
    private Random r = new Random();

    public void load() {
      try {
        cmp = new Compute();
        cmp.init(CL.TYPE_GPU);
        in1 = new float[SIZE_SIZE];
        in2 = new float[SIZE_SIZE];
        out = new float[SIZE_SIZE];
        for(int i=0;i<SIZE_SIZE;i++) {
          in1[i] = r.nextFloat();
          in2[i] = r.nextFloat();
        }
      } catch (Exception e) {
        JFLog.log(e);
      }
    }

    public void unload() {
      if (cmp != null) {
        cmp.uninit();
        cmp = null;
      }
    }

    public void test() {
      try {
        cmp.matrix_mult(SIZE, SIZE, SIZE, in1, in2, out);

        if (debug) System.out.print(Integer.toString(id));
      } catch (Throwable t) {
        JFLog.log(t);
      }
    }
  }
}
