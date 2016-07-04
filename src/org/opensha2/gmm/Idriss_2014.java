package org.opensha2.gmm;

import static java.lang.Math.log;
import static java.lang.Math.min;

import static org.opensha2.gmm.FaultStyle.REVERSE;
import static org.opensha2.gmm.GmmInput.Field.DIP;
import static org.opensha2.gmm.GmmInput.Field.MAG;
import static org.opensha2.gmm.GmmInput.Field.RAKE;
import static org.opensha2.gmm.GmmInput.Field.VS30;
import static org.opensha2.gmm.GmmInput.Field.Z1P0;
import static org.opensha2.gmm.GmmInput.Field.ZTOP;

import org.opensha2.eq.fault.Faults;
import org.opensha2.gmm.GmmInput.Constraints;

import com.google.common.collect.Range;

import java.util.Map;

/**
 * Implementation of the Idriss (2014) next generation ground motion model for
 * active crustal regions developed as part of <a
 * href="http://peer.berkeley.edu/ngawest2">NGA West II</a>.
 *
 * <p><b>Note:</b> Direct instantiation of {@code GroundMotionModel}s is
 * prohibited. Use {@link Gmm#instance(Imt)} to retrieve an instance for a
 * desired {@link Imt}.
 *
 * <p><b>Implementation notes:</b> Idriss (2014) recommends a cap of Vs=1200m/s
 * (implemented) and a distance limit of 150km (not implemented).
 *
 * <p><b>Implementation notes:</b> 0.01s SA values used for PGA.
 *
 * <p><b>Reference:</b> Idriss, I.M., 2014, An NGA-West2 empirical model for
 * estimating the horizontal spectral values generated by shallow crustal
 * earthquakes, Earthquake Spectra, v. 30, n. 3, p. 1155-1177.
 *
 * <p><b>doi:</b> <a href="http://dx.doi.org/10.1193/070613EQS195M">
 * 10.1193/070613EQS195M</a>
 *
 * <p><b>Component:</b> RotD50 (average horizontal)
 *
 * @author Peter Powers
 * @see Gmm#IDRISS_14
 */
public final class Idriss_2014 implements GroundMotionModel {

  static final String NAME = "Idriss (2014)";

  static final Constraints CONSTRAINTS = Constraints.builder()
      .set(MAG, Range.closed(5.0, 8.5))
      .setDistances(150.0)
      .set(DIP, Faults.DIP_RANGE)
      .set(ZTOP, Range.closed(0.0, 20.0))
      .set(RAKE, Faults.RAKE_RANGE)
      .set(VS30, Range.closedOpen(450.0, 1500.0))
      // TODO borrowed from ASK14
      .set(Z1P0, Range.closed(0.0, 3.0))
      .build();

  static final CoefficientContainer COEFFS = new CoefficientContainer("Idriss14.csv");

  private static final class Coefficients {

    final Imt imt;
    final double a1_lo, a2_lo, a1_hi, a2_hi, a3, b1_lo, b2_lo, b1_hi, b2_hi, ξ, γ, φ;

    Coefficients(Imt imt, CoefficientContainer cc) {
      this.imt = imt;
      Map<String, Double> coeffs = cc.get(imt);
      a1_lo = coeffs.get("a1_lo");
      a2_lo = coeffs.get("a2_lo");
      a1_hi = coeffs.get("a1_hi");
      a2_hi = coeffs.get("a2_hi");
      a3 = coeffs.get("a3");
      b1_lo = coeffs.get("b1_lo");
      b2_lo = coeffs.get("b2_lo");
      b1_hi = coeffs.get("b1_hi");
      b2_hi = coeffs.get("b2_hi");
      ξ = coeffs.get("xi");
      γ = coeffs.get("gamma");
      φ = coeffs.get("phi");
    }
  }

  private final Coefficients coeffs;

  Idriss_2014(Imt imt) {
    coeffs = new Coefficients(imt, COEFFS);
  }

  @Override
  public final ScalarGroundMotion calc(GmmInput in) {
    return calc(coeffs, in);
  }

  private static final ScalarGroundMotion calc(final Coefficients c, final GmmInput in) {

    double μ = calcMean(c, in);
    double σ = calcStdDev(c, in.Mw);

    return DefaultScalarGroundMotion.create(μ, σ);
  }

  // Mean ground motion model - cap of Vs = 1200 m/s
  private static final double calcMean(final Coefficients c, final GmmInput in) {

    double Mw = in.Mw;
    double rRup = in.rRup;

    FaultStyle style = GmmUtils.rakeToFaultStyle_NSHMP(in.rake);

    double a1 = c.a1_lo, a2 = c.a2_lo;
    double b1 = c.b1_lo, b2 = c.b2_lo;
    if (Mw > 6.75) {
      a1 = c.a1_hi;
      a2 = c.a2_hi;
      b1 = c.b1_hi;
      b2 = c.b2_hi;
    }

    return a1 + a2 * Mw + c.a3 * (8.5 - Mw) * (8.5 - Mw) - (b1 + b2 * Mw) * log(rRup + 10.0) +
        c.ξ * log(min(in.vs30, 1200.0)) + c.γ * rRup + (style == REVERSE ? c.φ : 0.0);
  }

  // Aleatory uncertainty model
  private static final double calcStdDev(final Coefficients c, final double Mw) {
    double s1 = 0.035;
    Double T = c.imt.period();
    s1 *= (T == null || T <= 0.05) ? log(0.05) : (T < 3.0) ? log(T) : log(3d);
    double s2 = 0.06;
    s2 *= (Mw <= 5.0) ? 5.0 : (Mw < 7.5) ? Mw : 7.5;
    return 1.18 + s1 - s2;
  }

}
