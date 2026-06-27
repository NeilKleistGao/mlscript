const definitionMetadata = globalThis.Symbol.for("mlscript.definitionMetadata");
const prettyPrint = globalThis.Symbol.for("mlscript.prettyPrint");
import runtime from "./../../Runtime.mjs";
import SpecialRegExpIPv4__Legacy from "./../SpecialRegExpIPv4.mjs";
let derive_Concat_sp_3, normalize_Altern_sp_13, matchImpl_SpecialRegExpIPv4_sp_7, normalize_Concat_sp_17, mkUnion_SpecialRegExpIPv4_sp_11, derive_Concat_sp_16, derive_Concat_sp_17, derive_Altern_sp_4, eq_Concat_sp_4, normalize_Altern_sp_10, concatUnique_SpecialRegExpIPv4_sp_17, startsWith_Concat_sp_14, concatUnique_SpecialRegExpIPv4_sp_0, eq_Altern_sp_1, normalize_Altern_sp_14, derive_Concat_sp_4, normalize_Altern_sp_16, concatUnique_SpecialRegExpIPv4_sp_46, concatUnique_SpecialRegExpIPv4_sp_41, normalize_Concat_sp_8, SpecialRegExpIPv41, mkUnion_SpecialRegExpIPv4_sp_8, normalize_Altern_sp_11, derive_Concat_sp_5, startsWith_Altern_sp_2, flat_Altern_sp_18, normalize_Altern_sp_12, startsWith_Concat_sp_16, match_SpecialRegExpIPv4_sp_0, derive_Concat_sp_19, normalize_Concat_sp_25, derive_Concat_sp_15, derive_In_sp_0, concatUnique_SpecialRegExpIPv4_sp_20, normalize_Altern_sp_2;
concatUnique_SpecialRegExpIPv4_sp_0 = function concatUnique_SpecialRegExpIPv4_sp_0(xs, ys) {
  let y, ys__, arg_Cons_0_1, arg_Cons_1_1, tmp22;
  if (ys instanceof SpecialRegExpIPv41.Cons.class) {
    let e, inlinedVal;
    arg_Cons_0_1 = ys.x;
    arg_Cons_1_1 = ys.xs;
    ys__ = arg_Cons_1_1;
    y = arg_Cons_0_1;
    e = y;
    inlinedVal = (new SpecialRegExpIPv41.Cons.class(e, SpecialRegExpIPv41.Nil));
    tmp22 = inlinedVal;
    return SpecialRegExpIPv41.concatUnique(tmp22, ys__)
  }
  return xs;
};
concatUnique_SpecialRegExpIPv4_sp_17 = function concatUnique_SpecialRegExpIPv4_sp_17(xs, ys) {
  let y, ys__, arg_Cons_0_1, arg_Cons_1_1, tmp22;
  if (ys instanceof SpecialRegExpIPv41.Cons.class) {
    let ls, e, inlinedVal, x2, scrut10, arg_Cons_0_, tmp21, self, other, inlinedVal1, p2__1, p1__3, scrut6, scrut7, arg_Concat_0_, arg_Concat_1_;
    arg_Cons_0_1 = ys.x;
    arg_Cons_1_1 = ys.xs;
    ys__ = arg_Cons_1_1;
    y = arg_Cons_0_1;
    ls = xs;
    e = y;
    arg_Cons_0_ = ls.x;
    ls.xs;
    x2 = arg_Cons_0_;
    self = x2;
    other = e;
    if (other instanceof SpecialRegExpIPv41.Concat.class) {
      let other1, inlinedVal2;
      arg_Concat_0_ = other.p1;
      arg_Concat_1_ = other.p2;
      p2__1 = arg_Concat_1_;
      p1__3 = arg_Concat_0_;
      self.p1;
      other1 = p1__3;
      if (other1 instanceof SpecialRegExpIPv41.Empty.class) {
        inlinedVal2 = true;
      } else {
        inlinedVal2 = false;
      }
      scrut6 = inlinedVal2;
      if (scrut6 === true) {
        scrut7 = eq_Concat_sp_4(self.p2, p2__1);
        if (scrut7 === true) {
          inlinedVal1 = true;
        } else {
          inlinedVal1 = false;
        }
      } else {
        inlinedVal1 = false;
      }
    } else {
      inlinedVal1 = false;
    }
    scrut10 = inlinedVal1;
    if (scrut10 === true) {
      inlinedVal = ls;
    } else {
      let e1, inlinedVal2;
      e1 = e;
      inlinedVal2 = (new SpecialRegExpIPv41.Cons.class(e1, SpecialRegExpIPv41.Nil));
      tmp21 = inlinedVal2;
      inlinedVal = (new SpecialRegExpIPv41.Cons.class(x2, tmp21));
    }
    tmp22 = inlinedVal;
    return SpecialRegExpIPv41.concatUnique(tmp22, ys__)
  }
  return xs;
};
concatUnique_SpecialRegExpIPv4_sp_20 = function concatUnique_SpecialRegExpIPv4_sp_20(xs, ys) {
  let y, ys__, arg_Cons_0_1, arg_Cons_1_1, tmp22;
  if (ys instanceof SpecialRegExpIPv41.Cons.class) {
    let ls, e, inlinedVal, x2, scrut10, arg_Cons_0_, tmp21, self, other, inlinedVal1, p2__1, p1__3, scrut6, scrut7, arg_Concat_0_, arg_Concat_1_;
    arg_Cons_0_1 = ys.x;
    arg_Cons_1_1 = ys.xs;
    ys__ = arg_Cons_1_1;
    y = arg_Cons_0_1;
    ls = xs;
    e = y;
    arg_Cons_0_ = ls.x;
    ls.xs;
    x2 = arg_Cons_0_;
    self = x2;
    other = e;
    if (other instanceof SpecialRegExpIPv41.Concat.class) {
      let other1, inlinedVal2;
      arg_Concat_0_ = other.p1;
      arg_Concat_1_ = other.p2;
      p2__1 = arg_Concat_1_;
      p1__3 = arg_Concat_0_;
      self.p1;
      other1 = p1__3;
      if (other1 instanceof SpecialRegExpIPv41.Nothing.class) {
        inlinedVal2 = true;
      } else {
        inlinedVal2 = false;
      }
      scrut6 = inlinedVal2;
      if (scrut6 === true) {
        scrut7 = eq_Concat_sp_4(self.p2, p2__1);
        if (scrut7 === true) {
          inlinedVal1 = true;
        } else {
          inlinedVal1 = false;
        }
      } else {
        inlinedVal1 = false;
      }
    } else {
      inlinedVal1 = false;
    }
    scrut10 = inlinedVal1;
    if (scrut10 === true) {
      inlinedVal = ls;
    } else {
      let e1, inlinedVal2;
      e1 = e;
      inlinedVal2 = (new SpecialRegExpIPv41.Cons.class(e1, SpecialRegExpIPv41.Nil));
      tmp21 = inlinedVal2;
      inlinedVal = (new SpecialRegExpIPv41.Cons.class(x2, tmp21));
    }
    tmp22 = inlinedVal;
    return SpecialRegExpIPv41.concatUnique(tmp22, ys__)
  }
  return xs;
};
concatUnique_SpecialRegExpIPv4_sp_41 = function concatUnique_SpecialRegExpIPv4_sp_41(xs, ys) {
  let y, ys__, arg_Cons_0_1, arg_Cons_1_1, tmp22, ls, e, inlinedVal, xs1, ys1, inlinedVal1, y1, arg_Cons_0_11, tmp221, ls1, e1, inlinedVal2, xs2, inlinedVal3, x2, xs11, arg_Cons_0_, arg_Cons_1_, tmp21, x21, arg_Cons_0_2, tmp211, e2, inlinedVal4, ls2, e3, inlinedVal5, x22, arg_Cons_0_3, tmp212, e4, inlinedVal6;
  arg_Cons_0_1 = ys.x;
  arg_Cons_1_1 = ys.xs;
  ys__ = arg_Cons_1_1;
  y = arg_Cons_0_1;
  ls = xs;
  e = y;
  arg_Cons_0_2 = ls.x;
  ls.xs;
  x21 = arg_Cons_0_2;
  e2 = e;
  inlinedVal4 = (new SpecialRegExpIPv41.Cons.class(e2, SpecialRegExpIPv41.Nil));
  tmp211 = inlinedVal4;
  inlinedVal = (new SpecialRegExpIPv41.Cons.class(x21, tmp211));
  tmp22 = inlinedVal;
  xs1 = tmp22;
  ys1 = ys__;
  arg_Cons_0_11 = ys1.x;
  ys1.xs;
  y1 = arg_Cons_0_11;
  ls1 = xs1;
  e1 = y1;
  arg_Cons_0_ = ls1.x;
  arg_Cons_1_ = ls1.xs;
  xs11 = arg_Cons_1_;
  x2 = arg_Cons_0_;
  ls2 = xs11;
  e3 = e1;
  arg_Cons_0_3 = ls2.x;
  ls2.xs;
  x22 = arg_Cons_0_3;
  e4 = e3;
  inlinedVal6 = (new SpecialRegExpIPv41.Cons.class(e4, SpecialRegExpIPv41.Nil));
  tmp212 = inlinedVal6;
  inlinedVal5 = (new SpecialRegExpIPv41.Cons.class(x22, tmp212));
  tmp21 = inlinedVal5;
  inlinedVal2 = (new SpecialRegExpIPv41.Cons.class(x2, tmp21));
  tmp221 = inlinedVal2;
  xs2 = tmp221;
  inlinedVal3 = xs2;
  inlinedVal1 = inlinedVal3;
  return inlinedVal1
};
concatUnique_SpecialRegExpIPv4_sp_46 = function concatUnique_SpecialRegExpIPv4_sp_46(xs, ys) {
  let y, arg_Cons_0_1, tmp22, ls, e, inlinedVal, xs1, inlinedVal1, x2, arg_Cons_0_, tmp21, e1, inlinedVal2;
  arg_Cons_0_1 = ys.x;
  ys.xs;
  y = arg_Cons_0_1;
  ls = xs;
  e = y;
  arg_Cons_0_ = ls.x;
  ls.xs;
  x2 = arg_Cons_0_;
  e1 = e;
  inlinedVal2 = (new SpecialRegExpIPv41.Cons.class(e1, SpecialRegExpIPv41.Nil));
  tmp21 = inlinedVal2;
  inlinedVal = (new SpecialRegExpIPv41.Cons.class(x2, tmp21));
  tmp22 = inlinedVal;
  xs1 = tmp22;
  inlinedVal1 = xs1;
  return inlinedVal1
};
matchImpl_SpecialRegExpIPv4_sp_7 = function matchImpl_SpecialRegExpIPv4_sp_7(p, s, acc) {
  let scrut15, c18, scrut17, tmp34, tmp35, tmp36, tmp37;
  tmp34 = SpecialRegExpIPv41.len(s);
  scrut15 = tmp34 == 0;
  if (scrut15 === true) {
    if (p instanceof SpecialRegExpIPv41.Altern.class) {
      return (new SpecialRegExpIPv41.None())
    } else if (p instanceof SpecialRegExpIPv41.Nothing.class) {
      return (new SpecialRegExpIPv41.None())
    }
    throw (new globalThis.Error("match error"));
  }
  if (p instanceof SpecialRegExpIPv41.Nothing.class) {
    return (new SpecialRegExpIPv41.None())
  }
  c18 = s[0];
  scrut17 = startsWith_Altern_sp_2(p, c18);
  if (scrut17 === true) {
    tmp35 = derive_Altern_sp_4(p, c18);
    tmp36 = runtime.safeCall(s.slice(1));
    tmp37 = acc + c18;
    return SpecialRegExpIPv41.matchImpl(tmp35, tmp36, tmp37)
  }
  return (new SpecialRegExpIPv41.None());
};
match_SpecialRegExpIPv4_sp_0 = function match_SpecialRegExpIPv4_sp_0(p, s) {
  let p1, s1, inlinedVal, scrut15, c18, scrut17, tmp34, tmp35, tmp36, tmp37;
  p1 = p;
  s1 = s;
  tmp34 = SpecialRegExpIPv41.len(s1);
  scrut15 = tmp34 == 0;
  if (scrut15 === true) {
    inlinedVal = (new SpecialRegExpIPv41.None());
    return inlinedVal
  }
  {
    let self, c, inlinedVal1, tmp16, self1, c1, inlinedVal2, tmp161, self2, c2, inlinedVal3, tmp162, self3, c3, inlinedVal4, tmp8, self4, c4, inlinedVal5, tmp163, c5, inlinedVal6;
    c18 = s1[0];
    self = p1;
    c = c18;
    self1 = self.p1;
    c1 = c;
    self2 = self1.p1;
    c2 = c1;
    self3 = self2.p1;
    c3 = c2;
    self4 = self3.p1;
    c4 = c3;
    self4.p1;
    c5 = c4;
    inlinedVal6 = "2" == c5;
    tmp163 = inlinedVal6;
    if (tmp163 === false) {
      self4.p1;
      inlinedVal5 = false;
    } else {
      inlinedVal5 = true;
    }
    tmp8 = inlinedVal5;
    if (tmp8 === false) {
      let self5, c6, inlinedVal7, tmp81;
      self5 = self3.p2;
      c6 = c3;
      tmp81 = runtime.safeCall(self5.p1.startsWith(c6));
      if (tmp81 === false) {
        inlinedVal7 = runtime.safeCall(self5.p2.startsWith(c6));
      } else {
        inlinedVal7 = true;
      }
      inlinedVal4 = inlinedVal7;
    } else {
      inlinedVal4 = true;
    }
    tmp162 = inlinedVal4;
    if (tmp162 === false) {
      self2.p1;
      inlinedVal3 = false;
    } else {
      inlinedVal3 = true;
    }
    tmp161 = inlinedVal3;
    if (tmp161 === false) {
      self1.p1;
      inlinedVal2 = false;
    } else {
      inlinedVal2 = true;
    }
    tmp16 = inlinedVal2;
    if (tmp16 === false) {
      self.p1;
      inlinedVal1 = false;
    } else {
      inlinedVal1 = true;
    }
    scrut17 = inlinedVal1;
    if (scrut17 === true) {
      let self5, c6, inlinedVal7, p2, s2, acc, inlinedVal8;
      self5 = p1;
      c6 = c18;
      inlinedLbl: {
        let p1__1, tmp14, arg$Concat$0$, arg$Concat$0$1, arg$Concat$1$, arg$Concat$0$2, arg$Concat$1$1, arg$Concat$0$3, arg$Concat$1$2, arg$Altern$0$, arg$Altern$1$, arg$Altern$0$1, arg$Altern$1$1, arg$Concat$0$4, arg$Concat$1$3, arg$Altern$0$2, arg$Altern$1$2, arg$Altern$0$3, arg$Altern$1$3, self6, c7, inlinedVal9, p1__11, tmp141, arg$Concat$0$5, self7, c8, inlinedVal10, p1__12, tmp142, self8, c9, inlinedVal11;
        self6 = self5.p1;
        c7 = c6;
        self7 = self6.p1;
        c8 = c7;
        self8 = self7.p1;
        c9 = c8;
        inlinedLbl1: {
          let tmp1, tmp2, tmp3, arg$Altern$0$4, arg$Concat$0$6, arg$Concat$1$4, self9, c10, inlinedVal12;
          tmp1 = derive_Concat_sp_3(self8.p1, c9);
          self9 = self8.p2;
          c10 = c9;
          inlinedLbl2: {
            let tmp11, tmp21, tmp31, arg$Altern$0$5, arg$Concat$0$7, arg$Concat$1$5;
            tmp11 = derive_Concat_sp_4(self9.p1, c10);
            tmp21 = derive_Concat_sp_5(self9.p2, c10);
            tmp31 = (new SpecialRegExpIPv41.Altern.class(tmp11, tmp21));
            arg$Altern$0$5 = tmp31.p1;
            if (arg$Altern$0$5 instanceof SpecialRegExpIPv41.Concat.class && (arg$Concat$0$7 = arg$Altern$0$5.p1, arg$Concat$1$5 = arg$Altern$0$5.p2, arg$Concat$0$7 instanceof SpecialRegExpIPv41.In.class) && arg$Concat$1$5 instanceof SpecialRegExpIPv41.In.class) {
              inlinedVal12 = normalize_Altern_sp_10(tmp31);
              break inlinedLbl2
            }
            inlinedVal12 = normalize_Altern_sp_11(tmp31);
          }
          tmp2 = inlinedVal12;
          tmp3 = (new SpecialRegExpIPv41.Altern.class(tmp1, tmp2));
          arg$Altern$0$4 = tmp3.p1;
          if (arg$Altern$0$4 instanceof SpecialRegExpIPv41.Concat.class && (arg$Concat$0$6 = arg$Altern$0$4.p1, arg$Concat$1$4 = arg$Altern$0$4.p2, arg$Concat$0$6 instanceof SpecialRegExpIPv41.Exact.class) && arg$Concat$1$4 instanceof SpecialRegExpIPv41.In.class) {
            inlinedVal11 = normalize_Altern_sp_12(tmp3);
            break inlinedLbl1
          }
          inlinedVal11 = normalize_Altern_sp_11(tmp3);
        }
        p1__12 = inlinedVal11;
        self7.p1;
        tmp142 = (new SpecialRegExpIPv41.Concat.class(p1__12, self7.p2));
        inlinedVal10 = normalize_Concat_sp_8(tmp142);
        p1__11 = inlinedVal10;
        self6.p1;
        tmp141 = (new SpecialRegExpIPv41.Concat.class(p1__11, self6.p2));
        arg$Concat$0$5 = tmp141.p1;
        if (arg$Concat$0$5 instanceof SpecialRegExpIPv41.Exact.class) {
          let self9, inlinedVal12, p1__2, tmp15, self10, inlinedVal13, self11, inlinedVal14, p1__21, tmp151, self12, inlinedVal15, self13, inlinedVal16, p1__22, tmp152, self14, inlinedVal17, p1__23, tmp153, self15, inlinedVal18;
          self9 = tmp141;
          self10 = self9.p1;
          inlinedVal13 = self10;
          p1__2 = inlinedVal13;
          self11 = self9.p2;
          self12 = self11.p1;
          p1__22 = normalize_Altern_sp_13(self12.p1);
          self14 = self12.p2;
          inlinedVal17 = self14;
          tmp152 = inlinedVal17;
          inlinedVal15 = (new SpecialRegExpIPv41.Concat.class(p1__22, tmp152));
          p1__21 = inlinedVal15;
          self13 = self11.p2;
          p1__23 = normalize_Altern_sp_13(self13.p1);
          self15 = self13.p2;
          inlinedVal18 = self15;
          tmp153 = inlinedVal18;
          inlinedVal16 = (new SpecialRegExpIPv41.Concat.class(p1__23, tmp153));
          tmp151 = inlinedVal16;
          inlinedVal14 = (new SpecialRegExpIPv41.Concat.class(p1__21, tmp151));
          tmp15 = inlinedVal14;
          inlinedVal12 = (new SpecialRegExpIPv41.Concat.class(p1__2, tmp15));
          inlinedVal9 = inlinedVal12;
        } else if (arg$Concat$0$5 instanceof SpecialRegExpIPv41.Nothing.class) {
          let self9, inlinedVal12, p1__2, self10, inlinedVal13;
          self9 = tmp141;
          self10 = self9.p1;
          inlinedVal13 = self10;
          p1__2 = inlinedVal13;
          inlinedVal12 = p1__2;
          inlinedVal9 = inlinedVal12;
        } else if (arg$Concat$0$5 instanceof SpecialRegExpIPv41.Concat.class) {
          let self9, inlinedVal12, p1__2, tmp15;
          self9 = tmp141;
          p1__2 = normalize_Concat_sp_8(self9.p1);
          if (p1__2 instanceof SpecialRegExpIPv41.Nothing.class) {
            inlinedVal12 = p1__2;
          } else {
            let self10, inlinedVal13, p1__21, tmp151;
            self10 = self9.p2;
            p1__21 = runtime.safeCall(self10.p1.normalize());
            if (p1__21 instanceof SpecialRegExpIPv41.Empty.class) {
              inlinedVal13 = runtime.safeCall(self10.p2.normalize());
            } else if (p1__21 instanceof SpecialRegExpIPv41.Nothing.class) {
              inlinedVal13 = p1__21;
            } else {
              tmp151 = runtime.safeCall(self10.p2.normalize());
              inlinedVal13 = (new SpecialRegExpIPv41.Concat.class(p1__21, tmp151));
            }
            tmp15 = inlinedVal13;
            inlinedVal12 = (new SpecialRegExpIPv41.Concat.class(p1__2, tmp15));
          }
          inlinedVal9 = inlinedVal12;
        } else {}
        p1__1 = inlinedVal9;
        self5.p1;
        tmp14 = (new SpecialRegExpIPv41.Concat.class(p1__1, self5.p2));
        arg$Concat$0$ = tmp14.p1;
        if (arg$Concat$0$ instanceof SpecialRegExpIPv41.Concat.class && (arg$Concat$0$1 = arg$Concat$0$.p1, arg$Concat$1$ = arg$Concat$0$.p2, arg$Concat$0$1 instanceof SpecialRegExpIPv41.Exact.class) && arg$Concat$1$ instanceof SpecialRegExpIPv41.Concat.class && (arg$Concat$0$2 = arg$Concat$1$.p1, arg$Concat$1$1 = arg$Concat$1$.p2, arg$Concat$0$2 instanceof SpecialRegExpIPv41.Concat.class) && (arg$Concat$0$3 = arg$Concat$0$2.p1, arg$Concat$1$2 = arg$Concat$0$2.p2, arg$Concat$0$3 instanceof SpecialRegExpIPv41.Altern.class) && (arg$Altern$0$ = arg$Concat$0$3.p1, arg$Altern$1$ = arg$Concat$0$3.p2, arg$Altern$0$ instanceof SpecialRegExpIPv41.Concat.class) && arg$Altern$1$ instanceof SpecialRegExpIPv41.Altern.class && (arg$Altern$0$1 = arg$Altern$1$.p1, arg$Altern$1$1 = arg$Altern$1$.p2, arg$Altern$0$1 instanceof SpecialRegExpIPv41.Concat.class) && arg$Altern$1$1 instanceof SpecialRegExpIPv41.Concat.class && arg$Concat$1$2 instanceof SpecialRegExpIPv41.Exact.class && arg$Concat$1$1 instanceof SpecialRegExpIPv41.Concat.class && (arg$Concat$0$4 = arg$Concat$1$1.p1, arg$Concat$1$3 = arg$Concat$1$1.p2, arg$Concat$0$4 instanceof SpecialRegExpIPv41.Altern.class) && (arg$Altern$0$2 = arg$Concat$0$4.p1, arg$Altern$1$2 = arg$Concat$0$4.p2, arg$Altern$0$2 instanceof SpecialRegExpIPv41.Concat.class) && arg$Altern$1$2 instanceof SpecialRegExpIPv41.Altern.class && (arg$Altern$0$3 = arg$Altern$1$2.p1, arg$Altern$1$3 = arg$Altern$1$2.p2, arg$Altern$0$3 instanceof SpecialRegExpIPv41.Concat.class) && arg$Altern$1$3 instanceof SpecialRegExpIPv41.Concat.class && arg$Concat$1$3 instanceof SpecialRegExpIPv41.Exact.class) {
          let self9, inlinedVal12, p1__2, tmp15, self10, inlinedVal13, p1__21, tmp151, self11, inlinedVal14;
          self9 = tmp14;
          self10 = self9.p1;
          self11 = self10.p1;
          inlinedVal14 = self11;
          p1__21 = inlinedVal14;
          tmp151 = normalize_Concat_sp_17(self10.p2);
          inlinedVal13 = (new SpecialRegExpIPv41.Concat.class(p1__21, tmp151));
          p1__2 = inlinedVal13;
          tmp15 = normalize_Altern_sp_13(self9.p2);
          inlinedVal12 = (new SpecialRegExpIPv41.Concat.class(p1__2, tmp15));
          inlinedVal7 = inlinedVal12;
          break inlinedLbl
        }
        if (arg$Concat$0$ instanceof SpecialRegExpIPv41.Nothing.class) {
          let self9, inlinedVal12, p1__2, self10, inlinedVal13;
          self9 = tmp14;
          self10 = self9.p1;
          inlinedVal13 = self10;
          p1__2 = inlinedVal13;
          inlinedVal12 = p1__2;
          inlinedVal7 = inlinedVal12;
        } else if (arg$Concat$0$ instanceof SpecialRegExpIPv41.Concat.class) {
          let self9, inlinedVal12, p1__2, tmp15, scrut, arg$Concat$0$6, self10, inlinedVal13;
          arg$Concat$0$1 = arg$Concat$0$.p1;
          arg$Concat$1$ = arg$Concat$0$.p2;
          self9 = tmp14;
          scrut = self9.p1;
          arg$Concat$0$6 = scrut.p1;
          if (arg$Concat$0$6 instanceof SpecialRegExpIPv41.Exact.class) {
            let self11, inlinedVal14, p1__21, tmp151;
            self11 = self9.p1;
            p1__21 = runtime.safeCall(self11.p1.normalize());
            if (p1__21 instanceof SpecialRegExpIPv41.Empty.class) {
              inlinedVal14 = runtime.safeCall(self11.p2.normalize());
            } else if (p1__21 instanceof SpecialRegExpIPv41.Nothing.class) {
              inlinedVal14 = p1__21;
            } else {
              tmp151 = runtime.safeCall(self11.p2.normalize());
              inlinedVal14 = (new SpecialRegExpIPv41.Concat.class(p1__21, tmp151));
            }
            p1__2 = inlinedVal14;
            if (p1__2 instanceof SpecialRegExpIPv41.Empty.class) {
              inlinedVal12 = normalize_Altern_sp_13(self9.p2);
            } else if (p1__2 instanceof SpecialRegExpIPv41.Nothing.class) {
              inlinedVal12 = p1__2;
            } else {
              tmp15 = normalize_Altern_sp_13(self9.p2);
              inlinedVal12 = (new SpecialRegExpIPv41.Concat.class(p1__2, tmp15));
            }
          } else {
            let p1__21, tmp151;
            self10 = self9.p1;
            p1__21 = runtime.safeCall(self10.p1.normalize());
            if (p1__21 instanceof SpecialRegExpIPv41.Empty.class) {
              inlinedVal13 = runtime.safeCall(self10.p2.normalize());
            } else if (p1__21 instanceof SpecialRegExpIPv41.Nothing.class) {
              inlinedVal13 = p1__21;
            } else {
              tmp151 = runtime.safeCall(self10.p2.normalize());
              inlinedVal13 = (new SpecialRegExpIPv41.Concat.class(p1__21, tmp151));
            }
            p1__2 = inlinedVal13;
            if (p1__2 instanceof SpecialRegExpIPv41.Empty.class) {
              inlinedVal12 = normalize_Altern_sp_13(self9.p2);
            } else if (p1__2 instanceof SpecialRegExpIPv41.Nothing.class) {
              inlinedVal12 = p1__2;
            } else {
              tmp15 = normalize_Altern_sp_13(self9.p2);
              inlinedVal12 = (new SpecialRegExpIPv41.Concat.class(p1__2, tmp15));
            }
          }
          inlinedVal7 = inlinedVal12;
        } else {}
      }
      tmp35 = inlinedVal7;
      tmp36 = runtime.safeCall(s1.slice(1));
      tmp37 = "" + c18;
      p2 = tmp35;
      s2 = tmp36;
      acc = tmp37;
      inlinedLbl1: {
        let scrut151, c181, scrut171, tmp341, tmp351, tmp361, tmp371, arg$Concat$0$, arg$Concat$0$1, arg$Concat$1$, arg$Concat$0$2, arg$Concat$1$1, arg$Concat$0$3, arg$Concat$1$2, arg$Altern$0$, arg$Altern$1$, arg$Altern$0$1, arg$Altern$1$1, arg$Concat$0$4, arg$Concat$1$3, arg$Altern$0$2, arg$Altern$1$2, arg$Altern$0$3, arg$Altern$1$3, arg$Concat$0$5, arg$Concat$0$6, arg$Concat$1$4, arg$Concat$0$7, arg$Concat$1$5, arg$Concat$0$8, arg$Concat$1$6, arg$Altern$0$4, arg$Altern$1$4, arg$Altern$0$5, arg$Altern$1$5, arg$Concat$0$9, arg$Concat$1$7, arg$Altern$0$6, arg$Altern$1$6, arg$Altern$0$7, arg$Altern$1$7;
        tmp341 = SpecialRegExpIPv41.len(s2);
        scrut151 = tmp341 == 0;
        if (scrut151 === true) {
          if (p2 instanceof SpecialRegExpIPv41.Concat.class) {
            arg$Concat$0$ = p2.p1;
            if (arg$Concat$0$ instanceof SpecialRegExpIPv41.Concat.class && (arg$Concat$0$1 = arg$Concat$0$.p1, arg$Concat$1$ = arg$Concat$0$.p2, arg$Concat$0$1 instanceof SpecialRegExpIPv41.Exact.class) && arg$Concat$1$ instanceof SpecialRegExpIPv41.Concat.class && (arg$Concat$0$2 = arg$Concat$1$.p1, arg$Concat$1$1 = arg$Concat$1$.p2, arg$Concat$0$2 instanceof SpecialRegExpIPv41.Concat.class) && (arg$Concat$0$3 = arg$Concat$0$2.p1, arg$Concat$1$2 = arg$Concat$0$2.p2, arg$Concat$0$3 instanceof SpecialRegExpIPv41.Altern.class) && (arg$Altern$0$ = arg$Concat$0$3.p1, arg$Altern$1$ = arg$Concat$0$3.p2, arg$Altern$0$ instanceof SpecialRegExpIPv41.Concat.class) && arg$Altern$1$ instanceof SpecialRegExpIPv41.Altern.class && (arg$Altern$0$1 = arg$Altern$1$.p1, arg$Altern$1$1 = arg$Altern$1$.p2, arg$Altern$0$1 instanceof SpecialRegExpIPv41.Concat.class) && arg$Altern$1$1 instanceof SpecialRegExpIPv41.Concat.class && arg$Concat$1$2 instanceof SpecialRegExpIPv41.Exact.class && arg$Concat$1$1 instanceof SpecialRegExpIPv41.Concat.class && (arg$Concat$0$4 = arg$Concat$1$1.p1, arg$Concat$1$3 = arg$Concat$1$1.p2, arg$Concat$0$4 instanceof SpecialRegExpIPv41.Altern.class) && (arg$Altern$0$2 = arg$Concat$0$4.p1, arg$Altern$1$2 = arg$Concat$0$4.p2, arg$Altern$0$2 instanceof SpecialRegExpIPv41.Concat.class) && arg$Altern$1$2 instanceof SpecialRegExpIPv41.Altern.class && (arg$Altern$0$3 = arg$Altern$1$2.p1, arg$Altern$1$3 = arg$Altern$1$2.p2, arg$Altern$0$3 instanceof SpecialRegExpIPv41.Concat.class) && arg$Altern$1$3 instanceof SpecialRegExpIPv41.Concat.class && arg$Concat$1$3 instanceof SpecialRegExpIPv41.Exact.class) {
              inlinedVal8 = (new SpecialRegExpIPv41.None());
              break inlinedLbl1
            }
            inlinedVal8 = (new SpecialRegExpIPv41.None());
          } else if (p2 instanceof SpecialRegExpIPv41.Altern.class) {
            inlinedVal8 = (new SpecialRegExpIPv41.None());
          } else if (p2 instanceof SpecialRegExpIPv41.Nothing.class) {
            inlinedVal8 = (new SpecialRegExpIPv41.None());
          } else {}
        } else {
          if (p2 instanceof SpecialRegExpIPv41.Concat.class) {
            arg$Concat$0$5 = p2.p1;
            if (arg$Concat$0$5 instanceof SpecialRegExpIPv41.Concat.class && (arg$Concat$0$6 = arg$Concat$0$5.p1, arg$Concat$1$4 = arg$Concat$0$5.p2, arg$Concat$0$6 instanceof SpecialRegExpIPv41.Exact.class) && arg$Concat$1$4 instanceof SpecialRegExpIPv41.Concat.class && (arg$Concat$0$7 = arg$Concat$1$4.p1, arg$Concat$1$5 = arg$Concat$1$4.p2, arg$Concat$0$7 instanceof SpecialRegExpIPv41.Concat.class) && (arg$Concat$0$8 = arg$Concat$0$7.p1, arg$Concat$1$6 = arg$Concat$0$7.p2, arg$Concat$0$8 instanceof SpecialRegExpIPv41.Altern.class) && (arg$Altern$0$4 = arg$Concat$0$8.p1, arg$Altern$1$4 = arg$Concat$0$8.p2, arg$Altern$0$4 instanceof SpecialRegExpIPv41.Concat.class) && arg$Altern$1$4 instanceof SpecialRegExpIPv41.Altern.class && (arg$Altern$0$5 = arg$Altern$1$4.p1, arg$Altern$1$5 = arg$Altern$1$4.p2, arg$Altern$0$5 instanceof SpecialRegExpIPv41.Concat.class) && arg$Altern$1$5 instanceof SpecialRegExpIPv41.Concat.class && arg$Concat$1$6 instanceof SpecialRegExpIPv41.Exact.class && arg$Concat$1$5 instanceof SpecialRegExpIPv41.Concat.class && (arg$Concat$0$9 = arg$Concat$1$5.p1, arg$Concat$1$7 = arg$Concat$1$5.p2, arg$Concat$0$9 instanceof SpecialRegExpIPv41.Altern.class) && (arg$Altern$0$6 = arg$Concat$0$9.p1, arg$Altern$1$6 = arg$Concat$0$9.p2, arg$Altern$0$6 instanceof SpecialRegExpIPv41.Concat.class) && arg$Altern$1$6 instanceof SpecialRegExpIPv41.Altern.class && (arg$Altern$0$7 = arg$Altern$1$6.p1, arg$Altern$1$7 = arg$Altern$1$6.p2, arg$Altern$0$7 instanceof SpecialRegExpIPv41.Concat.class) && arg$Altern$1$7 instanceof SpecialRegExpIPv41.Concat.class && arg$Concat$1$7 instanceof SpecialRegExpIPv41.Exact.class) {
              let self6, c7, inlinedVal9, tmp164, self7, c8, inlinedVal10, tmp165, c9, inlinedVal11;
              c181 = s2[0];
              self6 = p2;
              c7 = c181;
              self7 = self6.p1;
              c8 = c7;
              self7.p1;
              c9 = c8;
              inlinedVal11 = "." == c9;
              tmp165 = inlinedVal11;
              if (tmp165 === false) {
                self7.p1;
                inlinedVal10 = false;
              } else {
                inlinedVal10 = true;
              }
              tmp164 = inlinedVal10;
              if (tmp164 === false) {
                self6.p1;
                inlinedVal9 = false;
              } else {
                inlinedVal9 = true;
              }
              scrut171 = inlinedVal9;
              if (scrut171 === true) {
                let self8, c10, inlinedVal12, p3, s3, acc1, inlinedVal13, scrut152, c182, scrut172, tmp342, tmp352, tmp362, tmp372;
                self8 = p2;
                c10 = c181;
                inlinedLbl2: {
                  let p1__1, tmp14, arg$Concat$0$10, arg$Concat$0$11, arg$Concat$1$8, arg$Concat$0$12, arg$Concat$1$9, arg$Altern$0$8, arg$Altern$1$8, arg$Altern$0$9, arg$Altern$1$9, arg$Concat$0$13, arg$Concat$1$10, arg$Altern$0$10, arg$Altern$1$10, arg$Altern$0$11, arg$Altern$1$11, self9, c11, inlinedVal14, self10, inlinedVal15, p1__11, tmp141, arg$Concat$0$14, c12, inlinedVal16, self11, inlinedVal17, scrut, c13, inlinedVal18, p1__2, self12, inlinedVal19;
                  self9 = self8.p1;
                  c11 = c10;
                  self9.p1;
                  c12 = c11;
                  c13 = c12;
                  inlinedVal18 = "." == c13;
                  scrut = inlinedVal18;
                  if (scrut === true) {
                    inlinedVal16 = (new SpecialRegExpIPv41.Empty.class());
                  } else {
                    inlinedVal16 = (new SpecialRegExpIPv41.Nothing.class());
                  }
                  p1__11 = inlinedVal16;
                  self9.p1;
                  tmp141 = (new SpecialRegExpIPv41.Concat.class(p1__11, self9.p2));
                  arg$Concat$0$14 = tmp141.p1;
                  if (arg$Concat$0$14 instanceof SpecialRegExpIPv41.Empty.class) {
                    let self13, inlinedVal20;
                    self13 = tmp141;
                    self13.p1;
                    inlinedVal20 = normalize_Concat_sp_17(self13.p2);
                    inlinedVal14 = inlinedVal20;
                  } else {
                    let p1__21, self13, inlinedVal20;
                    self11 = tmp141;
                    self13 = self11.p1;
                    inlinedVal20 = self13;
                    p1__21 = inlinedVal20;
                    inlinedVal17 = p1__21;
                    inlinedVal14 = inlinedVal17;
                  }
                  p1__1 = inlinedVal14;
                  self8.p1;
                  tmp14 = (new SpecialRegExpIPv41.Concat.class(p1__1, self8.p2));
                  arg$Concat$0$10 = tmp14.p1;
                  if (arg$Concat$0$10 instanceof SpecialRegExpIPv41.Concat.class && (arg$Concat$0$11 = arg$Concat$0$10.p1, arg$Concat$1$8 = arg$Concat$0$10.p2, arg$Concat$0$11 instanceof SpecialRegExpIPv41.Concat.class) && (arg$Concat$0$12 = arg$Concat$0$11.p1, arg$Concat$1$9 = arg$Concat$0$11.p2, arg$Concat$0$12 instanceof SpecialRegExpIPv41.Altern.class) && (arg$Altern$0$8 = arg$Concat$0$12.p1, arg$Altern$1$8 = arg$Concat$0$12.p2, arg$Altern$0$8 instanceof SpecialRegExpIPv41.Concat.class) && arg$Altern$1$8 instanceof SpecialRegExpIPv41.Altern.class && (arg$Altern$0$9 = arg$Altern$1$8.p1, arg$Altern$1$9 = arg$Altern$1$8.p2, arg$Altern$0$9 instanceof SpecialRegExpIPv41.Concat.class) && arg$Altern$1$9 instanceof SpecialRegExpIPv41.Concat.class && arg$Concat$1$9 instanceof SpecialRegExpIPv41.Exact.class && arg$Concat$1$8 instanceof SpecialRegExpIPv41.Concat.class && (arg$Concat$0$13 = arg$Concat$1$8.p1, arg$Concat$1$10 = arg$Concat$1$8.p2, arg$Concat$0$13 instanceof SpecialRegExpIPv41.Altern.class) && (arg$Altern$0$10 = arg$Concat$0$13.p1, arg$Altern$1$10 = arg$Concat$0$13.p2, arg$Altern$0$10 instanceof SpecialRegExpIPv41.Concat.class) && arg$Altern$1$10 instanceof SpecialRegExpIPv41.Altern.class && (arg$Altern$0$11 = arg$Altern$1$10.p1, arg$Altern$1$11 = arg$Altern$1$10.p2, arg$Altern$0$11 instanceof SpecialRegExpIPv41.Concat.class) && arg$Altern$1$11 instanceof SpecialRegExpIPv41.Concat.class && arg$Concat$1$10 instanceof SpecialRegExpIPv41.Exact.class) {
                    let self13, inlinedVal20, p1__21, tmp15;
                    self13 = tmp14;
                    p1__21 = normalize_Concat_sp_17(self13.p1);
                    tmp15 = normalize_Altern_sp_14(self13.p2);
                    inlinedVal20 = (new SpecialRegExpIPv41.Concat.class(p1__21, tmp15));
                    inlinedVal12 = inlinedVal20;
                    break inlinedLbl2
                  }
                  self10 = tmp14;
                  self12 = self10.p1;
                  inlinedVal19 = self12;
                  p1__2 = inlinedVal19;
                  inlinedVal15 = p1__2;
                  inlinedVal12 = inlinedVal15;
                }
                tmp351 = inlinedVal12;
                tmp361 = runtime.safeCall(s2.slice(1));
                tmp371 = acc + c181;
                p3 = tmp351;
                s3 = tmp361;
                acc1 = tmp371;
                tmp342 = SpecialRegExpIPv41.len(s3);
                scrut152 = tmp342 == 0;
                if (scrut152 === true) {
                  if (p3 instanceof SpecialRegExpIPv41.Concat.class) {
                    inlinedVal13 = (new SpecialRegExpIPv41.None());
                  } else if (p3 instanceof SpecialRegExpIPv41.Nothing.class) {
                    inlinedVal13 = (new SpecialRegExpIPv41.None());
                  } else {
                    throw (new globalThis.Error("match error"))
                  }
                } else {
                  if (p3 instanceof SpecialRegExpIPv41.Nothing.class) {
                    inlinedVal13 = (new SpecialRegExpIPv41.None());
                  } else {
                    let self9, c11, inlinedVal14, tmp166, self10, c12, inlinedVal15, tmp167, self11, c13, inlinedVal16, tmp168;
                    c182 = s3[0];
                    self9 = p3;
                    c11 = c182;
                    self10 = self9.p1;
                    c12 = c11;
                    self11 = self10.p1;
                    c13 = c12;
                    tmp168 = startsWith_Altern_sp_2(self11.p1, c13);
                    if (tmp168 === false) {
                      self11.p1;
                      inlinedVal16 = false;
                    } else {
                      inlinedVal16 = true;
                    }
                    tmp167 = inlinedVal16;
                    if (tmp167 === false) {
                      self10.p1;
                      inlinedVal15 = false;
                    } else {
                      inlinedVal15 = true;
                    }
                    tmp166 = inlinedVal15;
                    if (tmp166 === false) {
                      self9.p1;
                      inlinedVal14 = false;
                    } else {
                      inlinedVal14 = true;
                    }
                    scrut172 = inlinedVal14;
                    if (scrut172 === true) {
                      let self12, c14, inlinedVal17, p4, s4, acc2, inlinedVal18;
                      self12 = p3;
                      c14 = c182;
                      inlinedLbl3: {
                        let p1__1, tmp14, arg$Concat$0$10, arg$Concat$0$11, arg$Concat$1$8, arg$Concat$0$12, arg$Concat$1$9, arg$Altern$0$8, arg$Altern$1$8, arg$Altern$0$9, arg$Altern$1$9, self13, c15, inlinedVal19, p1__11, tmp141, arg$Concat$0$13, self14, c16, inlinedVal20, p1__12, tmp142;
                        self13 = self12.p1;
                        c15 = c14;
                        self14 = self13.p1;
                        c16 = c15;
                        p1__12 = derive_Altern_sp_4(self14.p1, c16);
                        self14.p1;
                        tmp142 = (new SpecialRegExpIPv41.Concat.class(p1__12, self14.p2));
                        inlinedVal20 = normalize_Concat_sp_8(tmp142);
                        p1__11 = inlinedVal20;
                        self13.p1;
                        tmp141 = (new SpecialRegExpIPv41.Concat.class(p1__11, self13.p2));
                        arg$Concat$0$13 = tmp141.p1;
                        if (arg$Concat$0$13 instanceof SpecialRegExpIPv41.Exact.class) {
                          inlinedVal19 = normalize_Concat_sp_25(tmp141);
                        } else if (arg$Concat$0$13 instanceof SpecialRegExpIPv41.Nothing.class) {
                          let self15, inlinedVal21, p1__2, self16, inlinedVal22;
                          self15 = tmp141;
                          self16 = self15.p1;
                          inlinedVal22 = self16;
                          p1__2 = inlinedVal22;
                          inlinedVal21 = p1__2;
                          inlinedVal19 = inlinedVal21;
                        } else if (arg$Concat$0$13 instanceof SpecialRegExpIPv41.Concat.class) {
                          let self15, inlinedVal21, p1__2, tmp15;
                          self15 = tmp141;
                          p1__2 = normalize_Concat_sp_8(self15.p1);
                          if (p1__2 instanceof SpecialRegExpIPv41.Nothing.class) {
                            inlinedVal21 = p1__2;
                          } else {
                            let self16, inlinedVal22, p1__21, tmp151;
                            self16 = self15.p2;
                            p1__21 = runtime.safeCall(self16.p1.normalize());
                            if (p1__21 instanceof SpecialRegExpIPv41.Empty.class) {
                              inlinedVal22 = runtime.safeCall(self16.p2.normalize());
                            } else if (p1__21 instanceof SpecialRegExpIPv41.Nothing.class) {
                              inlinedVal22 = p1__21;
                            } else {
                              tmp151 = runtime.safeCall(self16.p2.normalize());
                              inlinedVal22 = (new SpecialRegExpIPv41.Concat.class(p1__21, tmp151));
                            }
                            tmp15 = inlinedVal22;
                            inlinedVal21 = (new SpecialRegExpIPv41.Concat.class(p1__2, tmp15));
                          }
                          inlinedVal19 = inlinedVal21;
                        } else {}
                        p1__1 = inlinedVal19;
                        self12.p1;
                        tmp14 = (new SpecialRegExpIPv41.Concat.class(p1__1, self12.p2));
                        arg$Concat$0$10 = tmp14.p1;
                        if (arg$Concat$0$10 instanceof SpecialRegExpIPv41.Concat.class && (arg$Concat$0$11 = arg$Concat$0$10.p1, arg$Concat$1$8 = arg$Concat$0$10.p2, arg$Concat$0$11 instanceof SpecialRegExpIPv41.Exact.class) && arg$Concat$1$8 instanceof SpecialRegExpIPv41.Concat.class && (arg$Concat$0$12 = arg$Concat$1$8.p1, arg$Concat$1$9 = arg$Concat$1$8.p2, arg$Concat$0$12 instanceof SpecialRegExpIPv41.Altern.class) && (arg$Altern$0$8 = arg$Concat$0$12.p1, arg$Altern$1$8 = arg$Concat$0$12.p2, arg$Altern$0$8 instanceof SpecialRegExpIPv41.Concat.class) && arg$Altern$1$8 instanceof SpecialRegExpIPv41.Altern.class && (arg$Altern$0$9 = arg$Altern$1$8.p1, arg$Altern$1$9 = arg$Altern$1$8.p2, arg$Altern$0$9 instanceof SpecialRegExpIPv41.Concat.class) && arg$Altern$1$9 instanceof SpecialRegExpIPv41.Concat.class && arg$Concat$1$9 instanceof SpecialRegExpIPv41.Exact.class) {
                          let self15, inlinedVal21, p1__2, tmp15;
                          self15 = tmp14;
                          p1__2 = normalize_Concat_sp_25(self15.p1);
                          tmp15 = normalize_Altern_sp_14(self15.p2);
                          inlinedVal21 = (new SpecialRegExpIPv41.Concat.class(p1__2, tmp15));
                          inlinedVal17 = inlinedVal21;
                          break inlinedLbl3
                        }
                        if (arg$Concat$0$10 instanceof SpecialRegExpIPv41.Nothing.class) {
                          let self15, inlinedVal21, p1__2, self16, inlinedVal22;
                          self15 = tmp14;
                          self16 = self15.p1;
                          inlinedVal22 = self16;
                          p1__2 = inlinedVal22;
                          inlinedVal21 = p1__2;
                          inlinedVal17 = inlinedVal21;
                        } else if (arg$Concat$0$10 instanceof SpecialRegExpIPv41.Concat.class) {
                          let self15, inlinedVal21, p1__2, tmp15, scrut, arg$Concat$0$14, self16, inlinedVal22;
                          arg$Concat$0$11 = arg$Concat$0$10.p1;
                          arg$Concat$1$8 = arg$Concat$0$10.p2;
                          self15 = tmp14;
                          scrut = self15.p1;
                          arg$Concat$0$14 = scrut.p1;
                          if (arg$Concat$0$14 instanceof SpecialRegExpIPv41.Exact.class) {
                            let self17, inlinedVal23, p1__21, tmp151;
                            self17 = self15.p1;
                            p1__21 = runtime.safeCall(self17.p1.normalize());
                            if (p1__21 instanceof SpecialRegExpIPv41.Empty.class) {
                              inlinedVal23 = runtime.safeCall(self17.p2.normalize());
                            } else if (p1__21 instanceof SpecialRegExpIPv41.Nothing.class) {
                              inlinedVal23 = p1__21;
                            } else {
                              tmp151 = runtime.safeCall(self17.p2.normalize());
                              inlinedVal23 = (new SpecialRegExpIPv41.Concat.class(p1__21, tmp151));
                            }
                            p1__2 = inlinedVal23;
                            if (p1__2 instanceof SpecialRegExpIPv41.Empty.class) {
                              inlinedVal21 = normalize_Altern_sp_14(self15.p2);
                            } else if (p1__2 instanceof SpecialRegExpIPv41.Nothing.class) {
                              inlinedVal21 = p1__2;
                            } else {
                              tmp15 = normalize_Altern_sp_14(self15.p2);
                              inlinedVal21 = (new SpecialRegExpIPv41.Concat.class(p1__2, tmp15));
                            }
                          } else {
                            let p1__21, tmp151;
                            self16 = self15.p1;
                            p1__21 = runtime.safeCall(self16.p1.normalize());
                            if (p1__21 instanceof SpecialRegExpIPv41.Empty.class) {
                              inlinedVal22 = runtime.safeCall(self16.p2.normalize());
                            } else if (p1__21 instanceof SpecialRegExpIPv41.Nothing.class) {
                              inlinedVal22 = p1__21;
                            } else {
                              tmp151 = runtime.safeCall(self16.p2.normalize());
                              inlinedVal22 = (new SpecialRegExpIPv41.Concat.class(p1__21, tmp151));
                            }
                            p1__2 = inlinedVal22;
                            if (p1__2 instanceof SpecialRegExpIPv41.Empty.class) {
                              inlinedVal21 = normalize_Altern_sp_14(self15.p2);
                            } else if (p1__2 instanceof SpecialRegExpIPv41.Nothing.class) {
                              inlinedVal21 = p1__2;
                            } else {
                              tmp15 = normalize_Altern_sp_14(self15.p2);
                              inlinedVal21 = (new SpecialRegExpIPv41.Concat.class(p1__2, tmp15));
                            }
                          }
                          inlinedVal17 = inlinedVal21;
                        } else {}
                      }
                      tmp352 = inlinedVal17;
                      tmp362 = runtime.safeCall(s3.slice(1));
                      tmp372 = acc1 + c182;
                      p4 = tmp352;
                      s4 = tmp362;
                      acc2 = tmp372;
                      inlinedLbl4: {
                        let scrut153, c183, scrut173, tmp343, tmp353, tmp363, tmp373, arg$Concat$0$10, arg$Concat$0$11, arg$Concat$1$8, arg$Concat$0$12, arg$Concat$1$9, arg$Altern$0$8, arg$Altern$1$8, arg$Altern$0$9, arg$Altern$1$9, arg$Concat$0$13, arg$Concat$0$14, arg$Concat$1$10, arg$Concat$0$15, arg$Concat$1$11, arg$Altern$0$10, arg$Altern$1$10, arg$Altern$0$11, arg$Altern$1$11;
                        tmp343 = SpecialRegExpIPv41.len(s4);
                        scrut153 = tmp343 == 0;
                        if (scrut153 === true) {
                          if (p4 instanceof SpecialRegExpIPv41.Concat.class) {
                            arg$Concat$0$10 = p4.p1;
                            if (arg$Concat$0$10 instanceof SpecialRegExpIPv41.Concat.class && (arg$Concat$0$11 = arg$Concat$0$10.p1, arg$Concat$1$8 = arg$Concat$0$10.p2, arg$Concat$0$11 instanceof SpecialRegExpIPv41.Exact.class) && arg$Concat$1$8 instanceof SpecialRegExpIPv41.Concat.class && (arg$Concat$0$12 = arg$Concat$1$8.p1, arg$Concat$1$9 = arg$Concat$1$8.p2, arg$Concat$0$12 instanceof SpecialRegExpIPv41.Altern.class) && (arg$Altern$0$8 = arg$Concat$0$12.p1, arg$Altern$1$8 = arg$Concat$0$12.p2, arg$Altern$0$8 instanceof SpecialRegExpIPv41.Concat.class) && arg$Altern$1$8 instanceof SpecialRegExpIPv41.Altern.class && (arg$Altern$0$9 = arg$Altern$1$8.p1, arg$Altern$1$9 = arg$Altern$1$8.p2, arg$Altern$0$9 instanceof SpecialRegExpIPv41.Concat.class) && arg$Altern$1$9 instanceof SpecialRegExpIPv41.Concat.class && arg$Concat$1$9 instanceof SpecialRegExpIPv41.Exact.class) {
                              inlinedVal18 = (new SpecialRegExpIPv41.None());
                              break inlinedLbl4
                            }
                            inlinedVal18 = (new SpecialRegExpIPv41.None());
                          } else if (p4 instanceof SpecialRegExpIPv41.Altern.class) {
                            inlinedVal18 = (new SpecialRegExpIPv41.None());
                          } else if (p4 instanceof SpecialRegExpIPv41.Nothing.class) {
                            inlinedVal18 = (new SpecialRegExpIPv41.None());
                          } else {}
                        } else {
                          if (p4 instanceof SpecialRegExpIPv41.Concat.class) {
                            arg$Concat$0$13 = p4.p1;
                            if (arg$Concat$0$13 instanceof SpecialRegExpIPv41.Concat.class && (arg$Concat$0$14 = arg$Concat$0$13.p1, arg$Concat$1$10 = arg$Concat$0$13.p2, arg$Concat$0$14 instanceof SpecialRegExpIPv41.Exact.class) && arg$Concat$1$10 instanceof SpecialRegExpIPv41.Concat.class && (arg$Concat$0$15 = arg$Concat$1$10.p1, arg$Concat$1$11 = arg$Concat$1$10.p2, arg$Concat$0$15 instanceof SpecialRegExpIPv41.Altern.class) && (arg$Altern$0$10 = arg$Concat$0$15.p1, arg$Altern$1$10 = arg$Concat$0$15.p2, arg$Altern$0$10 instanceof SpecialRegExpIPv41.Concat.class) && arg$Altern$1$10 instanceof SpecialRegExpIPv41.Altern.class && (arg$Altern$0$11 = arg$Altern$1$10.p1, arg$Altern$1$11 = arg$Altern$1$10.p2, arg$Altern$0$11 instanceof SpecialRegExpIPv41.Concat.class) && arg$Altern$1$11 instanceof SpecialRegExpIPv41.Concat.class && arg$Concat$1$11 instanceof SpecialRegExpIPv41.Exact.class) {
                              let self13, c15, inlinedVal19, tmp169, self14, c16, inlinedVal20, tmp1610, c17, inlinedVal21;
                              c183 = s4[0];
                              self13 = p4;
                              c15 = c183;
                              self14 = self13.p1;
                              c16 = c15;
                              self14.p1;
                              c17 = c16;
                              inlinedVal21 = "." == c17;
                              tmp1610 = inlinedVal21;
                              if (tmp1610 === false) {
                                self14.p1;
                                inlinedVal20 = false;
                              } else {
                                inlinedVal20 = true;
                              }
                              tmp169 = inlinedVal20;
                              if (tmp169 === false) {
                                self13.p1;
                                inlinedVal19 = false;
                              } else {
                                inlinedVal19 = true;
                              }
                              scrut173 = inlinedVal19;
                              if (scrut173 === true) {
                                let self15, c19, inlinedVal22, p5, s5, acc3, inlinedVal23, scrut154, c184, scrut174, tmp344, tmp354, tmp364, tmp374;
                                self15 = p4;
                                c19 = c183;
                                inlinedLbl5: {
                                  let p1__1, tmp14, arg$Concat$0$16, arg$Concat$0$17, arg$Concat$1$12, arg$Altern$0$12, arg$Altern$1$12, arg$Altern$0$13, arg$Altern$1$13, self16, c20, inlinedVal24, self17, inlinedVal25, p1__11, tmp141, arg$Concat$0$18, c21, inlinedVal26, self18, inlinedVal27, scrut, c22, inlinedVal28, p1__2, self19, inlinedVal29;
                                  self16 = self15.p1;
                                  c20 = c19;
                                  self16.p1;
                                  c21 = c20;
                                  c22 = c21;
                                  inlinedVal28 = "." == c22;
                                  scrut = inlinedVal28;
                                  if (scrut === true) {
                                    inlinedVal26 = (new SpecialRegExpIPv41.Empty.class());
                                  } else {
                                    inlinedVal26 = (new SpecialRegExpIPv41.Nothing.class());
                                  }
                                  p1__11 = inlinedVal26;
                                  self16.p1;
                                  tmp141 = (new SpecialRegExpIPv41.Concat.class(p1__11, self16.p2));
                                  arg$Concat$0$18 = tmp141.p1;
                                  if (arg$Concat$0$18 instanceof SpecialRegExpIPv41.Empty.class) {
                                    let self20, inlinedVal30, self21, inlinedVal31, p1__21, tmp15, self22, inlinedVal32;
                                    self20 = tmp141;
                                    self20.p1;
                                    self21 = self20.p2;
                                    p1__21 = normalize_Altern_sp_14(self21.p1);
                                    self22 = self21.p2;
                                    inlinedVal32 = self22;
                                    tmp15 = inlinedVal32;
                                    inlinedVal31 = (new SpecialRegExpIPv41.Concat.class(p1__21, tmp15));
                                    inlinedVal30 = inlinedVal31;
                                    inlinedVal24 = inlinedVal30;
                                  } else {
                                    let p1__21, self20, inlinedVal30;
                                    self18 = tmp141;
                                    self20 = self18.p1;
                                    inlinedVal30 = self20;
                                    p1__21 = inlinedVal30;
                                    inlinedVal27 = p1__21;
                                    inlinedVal24 = inlinedVal27;
                                  }
                                  p1__1 = inlinedVal24;
                                  self15.p1;
                                  tmp14 = (new SpecialRegExpIPv41.Concat.class(p1__1, self15.p2));
                                  arg$Concat$0$16 = tmp14.p1;
                                  if (arg$Concat$0$16 instanceof SpecialRegExpIPv41.Concat.class && (arg$Concat$0$17 = arg$Concat$0$16.p1, arg$Concat$1$12 = arg$Concat$0$16.p2, arg$Concat$0$17 instanceof SpecialRegExpIPv41.Altern.class) && (arg$Altern$0$12 = arg$Concat$0$17.p1, arg$Altern$1$12 = arg$Concat$0$17.p2, arg$Altern$0$12 instanceof SpecialRegExpIPv41.Concat.class) && arg$Altern$1$12 instanceof SpecialRegExpIPv41.Altern.class && (arg$Altern$0$13 = arg$Altern$1$12.p1, arg$Altern$1$13 = arg$Altern$1$12.p2, arg$Altern$0$13 instanceof SpecialRegExpIPv41.Concat.class) && arg$Altern$1$13 instanceof SpecialRegExpIPv41.Concat.class && arg$Concat$1$12 instanceof SpecialRegExpIPv41.Exact.class) {
                                    let self20, inlinedVal30, p1__21, tmp15, self21, inlinedVal31, p1__22, tmp151, self22, inlinedVal32;
                                    self20 = tmp14;
                                    self21 = self20.p1;
                                    p1__22 = normalize_Altern_sp_14(self21.p1);
                                    self22 = self21.p2;
                                    inlinedVal32 = self22;
                                    tmp151 = inlinedVal32;
                                    inlinedVal31 = (new SpecialRegExpIPv41.Concat.class(p1__22, tmp151));
                                    p1__21 = inlinedVal31;
                                    tmp15 = normalize_Altern_sp_14(self20.p2);
                                    inlinedVal30 = (new SpecialRegExpIPv41.Concat.class(p1__21, tmp15));
                                    inlinedVal22 = inlinedVal30;
                                    break inlinedLbl5
                                  }
                                  self17 = tmp14;
                                  self19 = self17.p1;
                                  inlinedVal29 = self19;
                                  p1__2 = inlinedVal29;
                                  inlinedVal25 = p1__2;
                                  inlinedVal22 = inlinedVal25;
                                }
                                tmp353 = inlinedVal22;
                                tmp363 = runtime.safeCall(s4.slice(1));
                                tmp373 = acc2 + c183;
                                p5 = tmp353;
                                s5 = tmp363;
                                acc3 = tmp373;
                                tmp344 = SpecialRegExpIPv41.len(s5);
                                scrut154 = tmp344 == 0;
                                if (scrut154 === true) {
                                  if (p5 instanceof SpecialRegExpIPv41.Concat.class) {
                                    inlinedVal23 = (new SpecialRegExpIPv41.None());
                                  } else if (p5 instanceof SpecialRegExpIPv41.Nothing.class) {
                                    inlinedVal23 = (new SpecialRegExpIPv41.None());
                                  } else {
                                    throw (new globalThis.Error("match error"))
                                  }
                                } else {
                                  if (p5 instanceof SpecialRegExpIPv41.Nothing.class) {
                                    inlinedVal23 = (new SpecialRegExpIPv41.None());
                                  } else {
                                    let self16, c20, inlinedVal24, tmp1611, self17, c21, inlinedVal25, tmp1612;
                                    c184 = s5[0];
                                    self16 = p5;
                                    c20 = c184;
                                    self17 = self16.p1;
                                    c21 = c20;
                                    tmp1612 = startsWith_Altern_sp_2(self17.p1, c21);
                                    if (tmp1612 === false) {
                                      self17.p1;
                                      inlinedVal25 = false;
                                    } else {
                                      inlinedVal25 = true;
                                    }
                                    tmp1611 = inlinedVal25;
                                    if (tmp1611 === false) {
                                      self16.p1;
                                      inlinedVal24 = false;
                                    } else {
                                      inlinedVal24 = true;
                                    }
                                    scrut174 = inlinedVal24;
                                    if (scrut174 === true) {
                                      let self18, c22, inlinedVal26, p6, s6, acc4, inlinedVal27, p1__1, tmp14, arg$Concat$0$16, self19, c23, inlinedVal28, p1__11, tmp141, scrut155, c185, scrut175, tmp345, tmp355, tmp365, tmp375, arg$Concat$0$17, arg$Concat$0$18;
                                      self18 = p5;
                                      c22 = c184;
                                      self19 = self18.p1;
                                      c23 = c22;
                                      p1__11 = derive_Altern_sp_4(self19.p1, c23);
                                      self19.p1;
                                      tmp141 = (new SpecialRegExpIPv41.Concat.class(p1__11, self19.p2));
                                      inlinedVal28 = normalize_Concat_sp_8(tmp141);
                                      p1__1 = inlinedVal28;
                                      self18.p1;
                                      tmp14 = (new SpecialRegExpIPv41.Concat.class(p1__1, self18.p2));
                                      arg$Concat$0$16 = tmp14.p1;
                                      if (arg$Concat$0$16 instanceof SpecialRegExpIPv41.Exact.class) {
                                        let self20, inlinedVal29, p1__2, tmp15, self21, inlinedVal30;
                                        self20 = tmp14;
                                        self21 = self20.p1;
                                        inlinedVal30 = self21;
                                        p1__2 = inlinedVal30;
                                        tmp15 = normalize_Altern_sp_14(self20.p2);
                                        inlinedVal29 = (new SpecialRegExpIPv41.Concat.class(p1__2, tmp15));
                                        inlinedVal26 = inlinedVal29;
                                      } else if (arg$Concat$0$16 instanceof SpecialRegExpIPv41.Nothing.class) {
                                        let self20, inlinedVal29, p1__2, self21, inlinedVal30;
                                        self20 = tmp14;
                                        self21 = self20.p1;
                                        inlinedVal30 = self21;
                                        p1__2 = inlinedVal30;
                                        inlinedVal29 = p1__2;
                                        inlinedVal26 = inlinedVal29;
                                      } else if (arg$Concat$0$16 instanceof SpecialRegExpIPv41.Concat.class) {
                                        let self20, inlinedVal29, p1__2, tmp15;
                                        self20 = tmp14;
                                        p1__2 = normalize_Concat_sp_8(self20.p1);
                                        if (p1__2 instanceof SpecialRegExpIPv41.Nothing.class) {
                                          inlinedVal29 = p1__2;
                                        } else {
                                          tmp15 = normalize_Altern_sp_14(self20.p2);
                                          inlinedVal29 = (new SpecialRegExpIPv41.Concat.class(p1__2, tmp15));
                                        }
                                        inlinedVal26 = inlinedVal29;
                                      } else {}
                                      tmp354 = inlinedVal26;
                                      tmp364 = runtime.safeCall(s5.slice(1));
                                      tmp374 = acc3 + c184;
                                      p6 = tmp354;
                                      s6 = tmp364;
                                      acc4 = tmp374;
                                      tmp345 = SpecialRegExpIPv41.len(s6);
                                      scrut155 = tmp345 == 0;
                                      if (scrut155 === true) {
                                        if (p6 instanceof SpecialRegExpIPv41.Concat.class) {
                                          arg$Concat$0$17 = p6.p1;
                                          if (arg$Concat$0$17 instanceof SpecialRegExpIPv41.Exact.class) {
                                            inlinedVal27 = (new SpecialRegExpIPv41.None());
                                          } else if (arg$Concat$0$17 instanceof SpecialRegExpIPv41.Exact.class) {
                                            inlinedVal27 = (new SpecialRegExpIPv41.None());
                                          } else if (arg$Concat$0$17 instanceof SpecialRegExpIPv41.Concat.class) {
                                            inlinedVal27 = (new SpecialRegExpIPv41.None());
                                          } else {}
                                        } else if (p6 instanceof SpecialRegExpIPv41.Nothing.class) {
                                          inlinedVal27 = (new SpecialRegExpIPv41.None());
                                        } else {}
                                      } else {
                                        if (p6 instanceof SpecialRegExpIPv41.Concat.class) {
                                          arg$Concat$0$18 = p6.p1;
                                          if (arg$Concat$0$18 instanceof SpecialRegExpIPv41.Exact.class) {
                                            let self20, c24, inlinedVal29, tmp1613, c25, inlinedVal30;
                                            c185 = s6[0];
                                            self20 = p6;
                                            c24 = c185;
                                            self20.p1;
                                            c25 = c24;
                                            inlinedVal30 = "." == c25;
                                            tmp1613 = inlinedVal30;
                                            if (tmp1613 === false) {
                                              self20.p1;
                                              inlinedVal29 = false;
                                            } else {
                                              inlinedVal29 = true;
                                            }
                                            scrut175 = inlinedVal29;
                                            if (scrut175 === true) {
                                              tmp355 = derive_Concat_sp_15(p6, c185);
                                              tmp365 = runtime.safeCall(s6.slice(1));
                                              tmp375 = acc4 + c185;
                                              inlinedVal27 = matchImpl_SpecialRegExpIPv4_sp_7(tmp355, tmp365, tmp375);
                                            } else {
                                              inlinedVal27 = (new SpecialRegExpIPv41.None());
                                            }
                                          } else if (arg$Concat$0$18 instanceof SpecialRegExpIPv41.Exact.class) {
                                            let self20, c24, inlinedVal29, tmp1613, c25, inlinedVal30;
                                            c185 = s6[0];
                                            self20 = p6;
                                            c24 = c185;
                                            self20.p1;
                                            c25 = c24;
                                            inlinedVal30 = "." == c25;
                                            tmp1613 = inlinedVal30;
                                            if (tmp1613 === false) {
                                              self20.p1;
                                              inlinedVal29 = false;
                                            } else {
                                              inlinedVal29 = true;
                                            }
                                            scrut175 = inlinedVal29;
                                            if (scrut175 === true) {
                                              tmp355 = derive_Concat_sp_15(p6, c185);
                                              tmp365 = runtime.safeCall(s6.slice(1));
                                              tmp375 = acc4 + c185;
                                              inlinedVal27 = matchImpl_SpecialRegExpIPv4_sp_7(tmp355, tmp365, tmp375);
                                            } else {
                                              inlinedVal27 = (new SpecialRegExpIPv41.None());
                                            }
                                          } else if (arg$Concat$0$18 instanceof SpecialRegExpIPv41.Concat.class) {
                                            let self20, c24, inlinedVal29, tmp1613;
                                            c185 = s6[0];
                                            self20 = p6;
                                            c24 = c185;
                                            tmp1613 = startsWith_Concat_sp_14(self20.p1, c24);
                                            if (tmp1613 === false) {
                                              self20.p1;
                                              inlinedVal29 = false;
                                            } else {
                                              inlinedVal29 = true;
                                            }
                                            scrut175 = inlinedVal29;
                                            if (scrut175 === true) {
                                              let p7, s7, acc5, inlinedVal30;
                                              tmp355 = derive_Concat_sp_16(p6, c185);
                                              tmp365 = runtime.safeCall(s6.slice(1));
                                              tmp375 = acc4 + c185;
                                              p7 = tmp355;
                                              s7 = tmp365;
                                              acc5 = tmp375;
                                              inlinedLbl6: {
                                                loopLabel: while (true) {
                                                  let scrut156, c186, scrut176, tmp346, tmp356, tmp366, tmp376, arg$Concat$0$19, arg$Altern$0$12, arg$Altern$1$12, arg$Concat$1$12, arg$Concat$0$20, arg$Altern$0$13, arg$Altern$1$13, arg$Concat$1$13;
                                                  tmp346 = SpecialRegExpIPv41.len(s7);
                                                  scrut156 = tmp346 == 0;
                                                  if (scrut156 === true) {
                                                    if (p7 instanceof SpecialRegExpIPv41.Concat.class) {
                                                      arg$Concat$0$19 = p7.p1;
                                                      if (arg$Concat$0$19 instanceof SpecialRegExpIPv41.Altern.class && (arg$Altern$0$12 = arg$Concat$0$19.p1, arg$Altern$1$12 = arg$Concat$0$19.p2, arg$Altern$0$12 instanceof SpecialRegExpIPv41.Concat.class) && (arg$Concat$1$12 = arg$Altern$0$12.p2, arg$Concat$1$12 instanceof SpecialRegExpIPv41.Exact.class) && arg$Altern$1$12 instanceof SpecialRegExpIPv41.Empty.class) {
                                                        inlinedVal30 = (new SpecialRegExpIPv41.None());
                                                        break inlinedLbl6
                                                      }
                                                      if (arg$Concat$0$19 instanceof SpecialRegExpIPv41.Exact.class) {
                                                        inlinedVal30 = (new SpecialRegExpIPv41.None());
                                                        break inlinedLbl6
                                                      } else if (arg$Concat$0$19 instanceof SpecialRegExpIPv41.Concat.class) {
                                                        inlinedVal30 = (new SpecialRegExpIPv41.None());
                                                        break inlinedLbl6
                                                      }
                                                    } else if (p7 instanceof SpecialRegExpIPv41.Nothing.class) {
                                                      inlinedVal30 = (new SpecialRegExpIPv41.None());
                                                      break inlinedLbl6
                                                    }
                                                  }
                                                  if (p7 instanceof SpecialRegExpIPv41.Concat.class) {
                                                    arg$Concat$0$20 = p7.p1;
                                                    if (arg$Concat$0$20 instanceof SpecialRegExpIPv41.Altern.class && (arg$Altern$0$13 = arg$Concat$0$20.p1, arg$Altern$1$13 = arg$Concat$0$20.p2, arg$Altern$0$13 instanceof SpecialRegExpIPv41.Concat.class) && (arg$Concat$1$13 = arg$Altern$0$13.p2, arg$Concat$1$13 instanceof SpecialRegExpIPv41.Exact.class) && arg$Altern$1$13 instanceof SpecialRegExpIPv41.Empty.class) {
                                                      let self21, c25, inlinedVal31, scrut9, tmp1614, self22, c26, inlinedVal32, tmp81;
                                                      c186 = s7[0];
                                                      self21 = p7;
                                                      c25 = c186;
                                                      self22 = self21.p1;
                                                      c26 = c25;
                                                      tmp81 = startsWith_Concat_sp_14(self22.p1, c26);
                                                      if (tmp81 === false) {
                                                        let inlinedVal33;
                                                        self22.p2;
                                                        inlinedVal33 = false;
                                                        inlinedVal32 = inlinedVal33;
                                                      } else {
                                                        inlinedVal32 = true;
                                                      }
                                                      tmp1614 = inlinedVal32;
                                                      if (tmp1614 === false) {
                                                        self21.p1;
                                                        scrut9 = startsWith_Altern_sp_2(self21.p2, c25);
                                                        if (scrut9 === true) {
                                                          inlinedVal31 = true;
                                                        } else {
                                                          inlinedVal31 = false;
                                                        }
                                                      } else {
                                                        inlinedVal31 = true;
                                                      }
                                                      scrut176 = inlinedVal31;
                                                      if (scrut176 === true) {
                                                        let self23, c27, inlinedVal33, p1__12, tmp11, tmp12, tmp13, self24, c28, inlinedVal34, self25, inlinedVal35, tmp5, self26, inlinedVal36;
                                                        self23 = p7;
                                                        c27 = c186;
                                                        self24 = self23.p1;
                                                        c28 = c27;
                                                        inlinedLbl7: {
                                                          let tmp1, tmp2, tmp3, arg$Altern$0$14, arg$Concat$1$14, arg$Altern$0$15, arg$Altern$1$14, arg$Concat$1$15, inlinedVal37;
                                                          tmp1 = derive_Concat_sp_17(self24.p1, c28);
                                                          self24.p2;
                                                          inlinedVal37 = (new SpecialRegExpIPv41.Nothing.class());
                                                          tmp2 = inlinedVal37;
                                                          tmp3 = (new SpecialRegExpIPv41.Altern.class(tmp1, tmp2));
                                                          arg$Altern$0$14 = tmp3.p1;
                                                          if (arg$Altern$0$14 instanceof SpecialRegExpIPv41.Altern.class && (arg$Altern$0$15 = arg$Altern$0$14.p1, arg$Altern$1$14 = arg$Altern$0$14.p2, arg$Altern$0$15 instanceof SpecialRegExpIPv41.Concat.class) && (arg$Concat$1$15 = arg$Altern$0$15.p2, arg$Concat$1$15 instanceof SpecialRegExpIPv41.Exact.class) && arg$Altern$1$14 instanceof SpecialRegExpIPv41.Empty.class) {
                                                            let self27, inlinedVal38, tmp51, self28, inlinedVal39, p1__, tmp10, ys, inlinedVal40, xs, inlinedVal41, y, ys__, arg_Cons_0_1, arg_Cons_1_1, tmp22, e, inlinedVal42;
                                                            self27 = tmp3;
                                                            self28 = self27;
                                                            p1__ = flat_Altern_sp_18(self28.p1);
                                                            self28.p2;
                                                            (new SpecialRegExpIPv41.Nil());
                                                            (new SpecialRegExpIPv41.Nil());
                                                            ys = p1__;
                                                            arg_Cons_0_1 = ys.x;
                                                            arg_Cons_1_1 = ys.xs;
                                                            ys__ = arg_Cons_1_1;
                                                            y = arg_Cons_0_1;
                                                            e = y;
                                                            inlinedVal42 = (new SpecialRegExpIPv41.Cons.class(e, SpecialRegExpIPv41.Nil));
                                                            tmp22 = inlinedVal42;
                                                            inlinedVal40 = concatUnique_SpecialRegExpIPv4_sp_46(tmp22, ys__);
                                                            tmp10 = inlinedVal40;
                                                            xs = tmp10;
                                                            inlinedVal41 = xs;
                                                            inlinedVal39 = inlinedVal41;
                                                            tmp51 = inlinedVal39;
                                                            inlinedVal38 = mkUnion_SpecialRegExpIPv4_sp_11(tmp51);
                                                            inlinedVal34 = inlinedVal38;
                                                            break inlinedLbl7
                                                          }
                                                          if (arg$Altern$0$14 instanceof SpecialRegExpIPv41.Concat.class) {
                                                            arg$Concat$1$14 = arg$Altern$0$14.p2;
                                                            if (arg$Concat$1$14 instanceof SpecialRegExpIPv41.Exact.class) {
                                                              inlinedVal34 = normalize_Altern_sp_16(tmp3);
                                                            } else {
                                                              inlinedVal34 = normalize_Altern_sp_16(tmp3);
                                                            }
                                                          } else if (arg$Altern$0$14 instanceof SpecialRegExpIPv41.Exact.class) {
                                                            let self27, inlinedVal38, tmp51, self28, inlinedVal39, ls, inlinedVal40, p1__, tmp10, self29, inlinedVal41, ys, inlinedVal42, xs, inlinedVal43, y, arg_Cons_0_1, tmp22, e, inlinedVal44, xs1, inlinedVal45, x4, arg_Cons_0_4;
                                                            self27 = tmp3;
                                                            self28 = self27;
                                                            self29 = self28.p1;
                                                            inlinedVal41 = (new SpecialRegExpIPv41.Cons.class(self29, SpecialRegExpIPv41.Nil));
                                                            p1__ = inlinedVal41;
                                                            self28.p2;
                                                            (new SpecialRegExpIPv41.Nil());
                                                            (new SpecialRegExpIPv41.Nil());
                                                            ys = p1__;
                                                            arg_Cons_0_1 = ys.x;
                                                            ys.xs;
                                                            y = arg_Cons_0_1;
                                                            e = y;
                                                            inlinedVal44 = (new SpecialRegExpIPv41.Cons.class(e, SpecialRegExpIPv41.Nil));
                                                            tmp22 = inlinedVal44;
                                                            xs1 = tmp22;
                                                            inlinedVal45 = xs1;
                                                            inlinedVal42 = inlinedVal45;
                                                            tmp10 = inlinedVal42;
                                                            xs = tmp10;
                                                            inlinedVal43 = xs;
                                                            inlinedVal39 = inlinedVal43;
                                                            tmp51 = inlinedVal39;
                                                            ls = tmp51;
                                                            arg_Cons_0_4 = ls.x;
                                                            ls.xs;
                                                            x4 = arg_Cons_0_4;
                                                            inlinedVal40 = x4;
                                                            inlinedVal38 = inlinedVal40;
                                                            inlinedVal34 = inlinedVal38;
                                                          } else if (arg$Altern$0$14 instanceof SpecialRegExpIPv41.Nothing.class) {
                                                            let self27, inlinedVal38, self28, inlinedVal39;
                                                            self27 = tmp3;
                                                            self28 = self27;
                                                            self28.p1;
                                                            (new SpecialRegExpIPv41.Nil());
                                                            self28.p2;
                                                            (new SpecialRegExpIPv41.Nil());
                                                            (new SpecialRegExpIPv41.Nil());
                                                            inlinedVal39 = (new SpecialRegExpIPv41.Nothing.class());
                                                            inlinedVal38 = inlinedVal39;
                                                            inlinedVal34 = inlinedVal38;
                                                          } else {}
                                                        }
                                                        p1__12 = inlinedVal34;
                                                        self23.p1;
                                                        tmp11 = (new SpecialRegExpIPv41.Concat.class(p1__12, self23.p2));
                                                        tmp12 = derive_Altern_sp_4(self23.p2, c27);
                                                        tmp13 = (new SpecialRegExpIPv41.Altern.class(tmp11, tmp12));
                                                        self25 = tmp13;
                                                        self26 = self25;
                                                        inlinedLbl8: {
                                                          let p1__, p2__, tmp10, scrut, arg$Concat$0$21, arg$Altern$0$14, arg$Altern$1$14, arg$Concat$1$14;
                                                          scrut = self26.p1;
                                                          arg$Concat$0$21 = scrut.p1;
                                                          if (arg$Concat$0$21 instanceof SpecialRegExpIPv41.Altern.class && (arg$Altern$0$14 = arg$Concat$0$21.p1, arg$Altern$1$14 = arg$Concat$0$21.p2, arg$Altern$0$14 instanceof SpecialRegExpIPv41.Concat.class) && (arg$Concat$1$14 = arg$Altern$0$14.p2, arg$Concat$1$14 instanceof SpecialRegExpIPv41.Exact.class) && arg$Altern$1$14 instanceof SpecialRegExpIPv41.Empty.class) {
                                                            let self27, inlinedVal37, ys, inlinedVal38, xs, ys1, inlinedVal39, y, arg_Cons_0_1, tmp22, e, inlinedVal40, xs1, inlinedVal41, y1, ys__, arg_Cons_0_11, arg_Cons_1_1, tmp221;
                                                            self27 = self26.p1;
                                                            inlinedVal37 = (new SpecialRegExpIPv41.Cons.class(self27, SpecialRegExpIPv41.Nil));
                                                            p1__ = inlinedVal37;
                                                            p2__ = runtime.safeCall(self26.p2.flat());
                                                            (new SpecialRegExpIPv41.Nil());
                                                            ys = p1__;
                                                            arg_Cons_0_1 = ys.x;
                                                            ys.xs;
                                                            y = arg_Cons_0_1;
                                                            e = y;
                                                            inlinedVal40 = (new SpecialRegExpIPv41.Cons.class(e, SpecialRegExpIPv41.Nil));
                                                            tmp22 = inlinedVal40;
                                                            xs1 = tmp22;
                                                            inlinedVal41 = xs1;
                                                            inlinedVal38 = inlinedVal41;
                                                            tmp10 = inlinedVal38;
                                                            xs = tmp10;
                                                            ys1 = p2__;
                                                            if (ys1 instanceof SpecialRegExpIPv41.Cons.class) {
                                                              let ls, e1, inlinedVal42, x2, scrut10, arg_Cons_0_, tmp21, self28, other, inlinedVal43, p2__1, p1__3, scrut6, scrut7, arg_Concat_0_, arg_Concat_1_;
                                                              arg_Cons_0_11 = ys1.x;
                                                              arg_Cons_1_1 = ys1.xs;
                                                              ys__ = arg_Cons_1_1;
                                                              y1 = arg_Cons_0_11;
                                                              ls = xs;
                                                              e1 = y1;
                                                              arg_Cons_0_ = ls.x;
                                                              ls.xs;
                                                              x2 = arg_Cons_0_;
                                                              self28 = x2;
                                                              other = e1;
                                                              if (other instanceof SpecialRegExpIPv41.Concat.class) {
                                                                let self29, other1, inlinedVal44, tmp6, tmp7;
                                                                arg_Concat_0_ = other.p1;
                                                                arg_Concat_1_ = other.p2;
                                                                p2__1 = arg_Concat_1_;
                                                                p1__3 = arg_Concat_0_;
                                                                self29 = self28.p1;
                                                                other1 = p1__3;
                                                                if (other1 instanceof SpecialRegExpIPv41.Altern.class) {
                                                                  let self30, inlinedVal45, self31, inlinedVal46, p1__2, p2__2, tmp9, tmp101, p1__4, p2__3, tmp91, tmp102;
                                                                  self30 = self29;
                                                                  p1__2 = runtime.safeCall(self30.p1.flat());
                                                                  p2__2 = runtime.safeCall(self30.p2.flat());
                                                                  tmp9 = (new SpecialRegExpIPv41.Nil());
                                                                  tmp101 = concatUnique_SpecialRegExpIPv4_sp_0(tmp9, p1__2);
                                                                  inlinedVal45 = SpecialRegExpIPv41.concatUnique(tmp101, p2__2);
                                                                  tmp6 = inlinedVal45;
                                                                  self31 = other1;
                                                                  p1__4 = runtime.safeCall(self31.p1.flat());
                                                                  p2__3 = runtime.safeCall(self31.p2.flat());
                                                                  tmp91 = (new SpecialRegExpIPv41.Nil());
                                                                  tmp102 = concatUnique_SpecialRegExpIPv4_sp_0(tmp91, p1__4);
                                                                  inlinedVal46 = SpecialRegExpIPv41.concatUnique(tmp102, p2__3);
                                                                  tmp7 = inlinedVal46;
                                                                  inlinedVal44 = SpecialRegExpIPv41.lsEq(tmp6, tmp7);
                                                                } else {
                                                                  inlinedVal44 = false;
                                                                }
                                                                scrut6 = inlinedVal44;
                                                                if (scrut6 === true) {
                                                                  scrut7 = eq_Altern_sp_1(self28.p2, p2__1);
                                                                  if (scrut7 === true) {
                                                                    inlinedVal43 = true;
                                                                  } else {
                                                                    inlinedVal43 = false;
                                                                  }
                                                                } else {
                                                                  inlinedVal43 = false;
                                                                }
                                                              } else {
                                                                inlinedVal43 = false;
                                                              }
                                                              scrut10 = inlinedVal43;
                                                              if (scrut10 === true) {
                                                                inlinedVal42 = ls;
                                                              } else {
                                                                let e2, inlinedVal44;
                                                                e2 = e1;
                                                                inlinedVal44 = (new SpecialRegExpIPv41.Cons.class(e2, SpecialRegExpIPv41.Nil));
                                                                tmp21 = inlinedVal44;
                                                                inlinedVal42 = (new SpecialRegExpIPv41.Cons.class(x2, tmp21));
                                                              }
                                                              tmp221 = inlinedVal42;
                                                              inlinedVal39 = SpecialRegExpIPv41.concatUnique(tmp221, ys__);
                                                            } else {
                                                              inlinedVal39 = xs;
                                                            }
                                                            inlinedVal36 = inlinedVal39;
                                                            break inlinedLbl8
                                                          }
                                                          if (arg$Concat$0$21 instanceof SpecialRegExpIPv41.Concat.class) {
                                                            let self27, inlinedVal37, ys, inlinedVal38, xs, ys1, inlinedVal39, y, arg_Cons_0_1, tmp22, e, inlinedVal40, xs1, inlinedVal41, y1, ys__, arg_Cons_0_11, arg_Cons_1_1, tmp221;
                                                            self27 = self26.p1;
                                                            inlinedVal37 = (new SpecialRegExpIPv41.Cons.class(self27, SpecialRegExpIPv41.Nil));
                                                            p1__ = inlinedVal37;
                                                            p2__ = runtime.safeCall(self26.p2.flat());
                                                            (new SpecialRegExpIPv41.Nil());
                                                            ys = p1__;
                                                            arg_Cons_0_1 = ys.x;
                                                            ys.xs;
                                                            y = arg_Cons_0_1;
                                                            e = y;
                                                            inlinedVal40 = (new SpecialRegExpIPv41.Cons.class(e, SpecialRegExpIPv41.Nil));
                                                            tmp22 = inlinedVal40;
                                                            xs1 = tmp22;
                                                            inlinedVal41 = xs1;
                                                            inlinedVal38 = inlinedVal41;
                                                            tmp10 = inlinedVal38;
                                                            xs = tmp10;
                                                            ys1 = p2__;
                                                            if (ys1 instanceof SpecialRegExpIPv41.Cons.class) {
                                                              let ls, e1, inlinedVal42, x2, scrut10, arg_Cons_0_, tmp21, self28, other, inlinedVal43, p2__1, p1__3, scrut6, scrut7, arg_Concat_0_, arg_Concat_1_;
                                                              arg_Cons_0_11 = ys1.x;
                                                              arg_Cons_1_1 = ys1.xs;
                                                              ys__ = arg_Cons_1_1;
                                                              y1 = arg_Cons_0_11;
                                                              ls = xs;
                                                              e1 = y1;
                                                              arg_Cons_0_ = ls.x;
                                                              ls.xs;
                                                              x2 = arg_Cons_0_;
                                                              self28 = x2;
                                                              other = e1;
                                                              if (other instanceof SpecialRegExpIPv41.Concat.class) {
                                                                arg_Concat_0_ = other.p1;
                                                                arg_Concat_1_ = other.p2;
                                                                p2__1 = arg_Concat_1_;
                                                                p1__3 = arg_Concat_0_;
                                                                scrut6 = eq_Concat_sp_4(self28.p1, p1__3);
                                                                if (scrut6 === true) {
                                                                  scrut7 = eq_Altern_sp_1(self28.p2, p2__1);
                                                                  if (scrut7 === true) {
                                                                    inlinedVal43 = true;
                                                                  } else {
                                                                    inlinedVal43 = false;
                                                                  }
                                                                } else {
                                                                  inlinedVal43 = false;
                                                                }
                                                              } else {
                                                                inlinedVal43 = false;
                                                              }
                                                              scrut10 = inlinedVal43;
                                                              if (scrut10 === true) {
                                                                inlinedVal42 = ls;
                                                              } else {
                                                                let e2, inlinedVal44;
                                                                e2 = e1;
                                                                inlinedVal44 = (new SpecialRegExpIPv41.Cons.class(e2, SpecialRegExpIPv41.Nil));
                                                                tmp21 = inlinedVal44;
                                                                inlinedVal42 = (new SpecialRegExpIPv41.Cons.class(x2, tmp21));
                                                              }
                                                              tmp221 = inlinedVal42;
                                                              inlinedVal39 = SpecialRegExpIPv41.concatUnique(tmp221, ys__);
                                                            } else {
                                                              inlinedVal39 = xs;
                                                            }
                                                            inlinedVal36 = inlinedVal39;
                                                          } else if (arg$Concat$0$21 instanceof SpecialRegExpIPv41.Exact.class) {
                                                            let self27, inlinedVal37, ys, inlinedVal38, xs, ys1, inlinedVal39, y, arg_Cons_0_1, tmp22, e, inlinedVal40, xs1, inlinedVal41, y1, ys__, arg_Cons_0_11, arg_Cons_1_1, tmp221;
                                                            self27 = self26.p1;
                                                            inlinedVal37 = (new SpecialRegExpIPv41.Cons.class(self27, SpecialRegExpIPv41.Nil));
                                                            p1__ = inlinedVal37;
                                                            p2__ = runtime.safeCall(self26.p2.flat());
                                                            (new SpecialRegExpIPv41.Nil());
                                                            ys = p1__;
                                                            arg_Cons_0_1 = ys.x;
                                                            ys.xs;
                                                            y = arg_Cons_0_1;
                                                            e = y;
                                                            inlinedVal40 = (new SpecialRegExpIPv41.Cons.class(e, SpecialRegExpIPv41.Nil));
                                                            tmp22 = inlinedVal40;
                                                            xs1 = tmp22;
                                                            inlinedVal41 = xs1;
                                                            inlinedVal38 = inlinedVal41;
                                                            tmp10 = inlinedVal38;
                                                            xs = tmp10;
                                                            ys1 = p2__;
                                                            if (ys1 instanceof SpecialRegExpIPv41.Cons.class) {
                                                              let ls, e1, inlinedVal42, x2, scrut10, arg_Cons_0_, tmp21, self28, other, inlinedVal43, p2__1, p1__3, scrut6, scrut7, arg_Concat_0_, arg_Concat_1_;
                                                              arg_Cons_0_11 = ys1.x;
                                                              arg_Cons_1_1 = ys1.xs;
                                                              ys__ = arg_Cons_1_1;
                                                              y1 = arg_Cons_0_11;
                                                              ls = xs;
                                                              e1 = y1;
                                                              arg_Cons_0_ = ls.x;
                                                              ls.xs;
                                                              x2 = arg_Cons_0_;
                                                              self28 = x2;
                                                              other = e1;
                                                              if (other instanceof SpecialRegExpIPv41.Concat.class) {
                                                                let other1, inlinedVal44, ch__, arg_Exact_0_;
                                                                arg_Concat_0_ = other.p1;
                                                                arg_Concat_1_ = other.p2;
                                                                p2__1 = arg_Concat_1_;
                                                                p1__3 = arg_Concat_0_;
                                                                self28.p1;
                                                                other1 = p1__3;
                                                                if (other1 instanceof SpecialRegExpIPv41.Exact.class) {
                                                                  arg_Exact_0_ = other1.ch;
                                                                  ch__ = arg_Exact_0_;
                                                                  inlinedVal44 = "." == ch__;
                                                                } else {
                                                                  inlinedVal44 = false;
                                                                }
                                                                scrut6 = inlinedVal44;
                                                                if (scrut6 === true) {
                                                                  scrut7 = eq_Altern_sp_1(self28.p2, p2__1);
                                                                  if (scrut7 === true) {
                                                                    inlinedVal43 = true;
                                                                  } else {
                                                                    inlinedVal43 = false;
                                                                  }
                                                                } else {
                                                                  inlinedVal43 = false;
                                                                }
                                                              } else {
                                                                inlinedVal43 = false;
                                                              }
                                                              scrut10 = inlinedVal43;
                                                              if (scrut10 === true) {
                                                                inlinedVal42 = ls;
                                                              } else {
                                                                let e2, inlinedVal44;
                                                                e2 = e1;
                                                                inlinedVal44 = (new SpecialRegExpIPv41.Cons.class(e2, SpecialRegExpIPv41.Nil));
                                                                tmp21 = inlinedVal44;
                                                                inlinedVal42 = (new SpecialRegExpIPv41.Cons.class(x2, tmp21));
                                                              }
                                                              tmp221 = inlinedVal42;
                                                              inlinedVal39 = SpecialRegExpIPv41.concatUnique(tmp221, ys__);
                                                            } else {
                                                              inlinedVal39 = xs;
                                                            }
                                                            inlinedVal36 = inlinedVal39;
                                                          } else if (arg$Concat$0$21 instanceof SpecialRegExpIPv41.Nothing.class) {
                                                            let self27, inlinedVal37, ys, inlinedVal38, xs, ys1, inlinedVal39, y, arg_Cons_0_1, tmp22, e, inlinedVal40, xs1, inlinedVal41, y1, ys__, arg_Cons_0_11, arg_Cons_1_1, tmp221;
                                                            self27 = self26.p1;
                                                            inlinedVal37 = (new SpecialRegExpIPv41.Cons.class(self27, SpecialRegExpIPv41.Nil));
                                                            p1__ = inlinedVal37;
                                                            p2__ = runtime.safeCall(self26.p2.flat());
                                                            (new SpecialRegExpIPv41.Nil());
                                                            ys = p1__;
                                                            arg_Cons_0_1 = ys.x;
                                                            ys.xs;
                                                            y = arg_Cons_0_1;
                                                            e = y;
                                                            inlinedVal40 = (new SpecialRegExpIPv41.Cons.class(e, SpecialRegExpIPv41.Nil));
                                                            tmp22 = inlinedVal40;
                                                            xs1 = tmp22;
                                                            inlinedVal41 = xs1;
                                                            inlinedVal38 = inlinedVal41;
                                                            tmp10 = inlinedVal38;
                                                            xs = tmp10;
                                                            ys1 = p2__;
                                                            if (ys1 instanceof SpecialRegExpIPv41.Cons.class) {
                                                              let ls, e1, inlinedVal42, x2, scrut10, arg_Cons_0_, tmp21, self28, other, inlinedVal43, p2__1, p1__3, scrut6, scrut7, arg_Concat_0_, arg_Concat_1_;
                                                              arg_Cons_0_11 = ys1.x;
                                                              arg_Cons_1_1 = ys1.xs;
                                                              ys__ = arg_Cons_1_1;
                                                              y1 = arg_Cons_0_11;
                                                              ls = xs;
                                                              e1 = y1;
                                                              arg_Cons_0_ = ls.x;
                                                              ls.xs;
                                                              x2 = arg_Cons_0_;
                                                              self28 = x2;
                                                              other = e1;
                                                              if (other instanceof SpecialRegExpIPv41.Concat.class) {
                                                                let other1, inlinedVal44;
                                                                arg_Concat_0_ = other.p1;
                                                                arg_Concat_1_ = other.p2;
                                                                p2__1 = arg_Concat_1_;
                                                                p1__3 = arg_Concat_0_;
                                                                self28.p1;
                                                                other1 = p1__3;
                                                                if (other1 instanceof SpecialRegExpIPv41.Nothing.class) {
                                                                  inlinedVal44 = true;
                                                                } else {
                                                                  inlinedVal44 = false;
                                                                }
                                                                scrut6 = inlinedVal44;
                                                                if (scrut6 === true) {
                                                                  scrut7 = eq_Altern_sp_1(self28.p2, p2__1);
                                                                  if (scrut7 === true) {
                                                                    inlinedVal43 = true;
                                                                  } else {
                                                                    inlinedVal43 = false;
                                                                  }
                                                                } else {
                                                                  inlinedVal43 = false;
                                                                }
                                                              } else {
                                                                inlinedVal43 = false;
                                                              }
                                                              scrut10 = inlinedVal43;
                                                              if (scrut10 === true) {
                                                                inlinedVal42 = ls;
                                                              } else {
                                                                let e2, inlinedVal44;
                                                                e2 = e1;
                                                                inlinedVal44 = (new SpecialRegExpIPv41.Cons.class(e2, SpecialRegExpIPv41.Nil));
                                                                tmp21 = inlinedVal44;
                                                                inlinedVal42 = (new SpecialRegExpIPv41.Cons.class(x2, tmp21));
                                                              }
                                                              tmp221 = inlinedVal42;
                                                              inlinedVal39 = SpecialRegExpIPv41.concatUnique(tmp221, ys__);
                                                            } else {
                                                              inlinedVal39 = xs;
                                                            }
                                                            inlinedVal36 = inlinedVal39;
                                                          } else {}
                                                        }
                                                        tmp5 = inlinedVal36;
                                                        inlinedVal35 = SpecialRegExpIPv41.mkUnion(tmp5);
                                                        inlinedVal33 = inlinedVal35;
                                                        tmp356 = inlinedVal33;
                                                        tmp366 = runtime.safeCall(s7.slice(1));
                                                        tmp376 = acc5 + c186;
                                                        inlinedVal30 = SpecialRegExpIPv41.matchImpl(tmp356, tmp366, tmp376);
                                                        break inlinedLbl6
                                                      }
                                                      inlinedVal30 = (new SpecialRegExpIPv41.None());
                                                      break inlinedLbl6;
                                                    }
                                                    if (arg$Concat$0$20 instanceof SpecialRegExpIPv41.Exact.class) {
                                                      let self21, c25, inlinedVal31, tmp1614, c26, inlinedVal32;
                                                      c186 = s7[0];
                                                      self21 = p7;
                                                      c25 = c186;
                                                      self21.p1;
                                                      c26 = c25;
                                                      inlinedVal32 = "." == c26;
                                                      tmp1614 = inlinedVal32;
                                                      if (tmp1614 === false) {
                                                        self21.p1;
                                                        inlinedVal31 = false;
                                                      } else {
                                                        inlinedVal31 = true;
                                                      }
                                                      scrut176 = inlinedVal31;
                                                      if (scrut176 === true) {
                                                        tmp356 = derive_Concat_sp_15(p7, c186);
                                                        tmp366 = runtime.safeCall(s7.slice(1));
                                                        tmp376 = acc5 + c186;
                                                        inlinedVal30 = matchImpl_SpecialRegExpIPv4_sp_7(tmp356, tmp366, tmp376);
                                                        break inlinedLbl6
                                                      }
                                                      inlinedVal30 = (new SpecialRegExpIPv41.None());
                                                      break inlinedLbl6;
                                                    } else if (arg$Concat$0$20 instanceof SpecialRegExpIPv41.Concat.class) {
                                                      let self21, c25, inlinedVal31, tmp1614;
                                                      c186 = s7[0];
                                                      self21 = p7;
                                                      c25 = c186;
                                                      tmp1614 = startsWith_Concat_sp_14(self21.p1, c25);
                                                      if (tmp1614 === false) {
                                                        self21.p1;
                                                        inlinedVal31 = false;
                                                      } else {
                                                        inlinedVal31 = true;
                                                      }
                                                      scrut176 = inlinedVal31;
                                                      if (scrut176 === true) {
                                                        tmp356 = derive_Concat_sp_16(p7, c186);
                                                        tmp366 = runtime.safeCall(s7.slice(1));
                                                        tmp376 = acc5 + c186;
                                                        p7 = tmp356;
                                                        s7 = tmp366;
                                                        acc5 = tmp376;
                                                        continue loopLabel
                                                      }
                                                      inlinedVal30 = (new SpecialRegExpIPv41.None());
                                                      break inlinedLbl6;
                                                    }
                                                  } else if (p7 instanceof SpecialRegExpIPv41.Nothing.class) {
                                                    inlinedVal30 = (new SpecialRegExpIPv41.None());
                                                    break inlinedLbl6
                                                  }
                                                }
                                              }
                                              inlinedVal27 = inlinedVal30;
                                            } else {
                                              inlinedVal27 = (new SpecialRegExpIPv41.None());
                                            }
                                          } else {}
                                        } else if (p6 instanceof SpecialRegExpIPv41.Nothing.class) {
                                          inlinedVal27 = (new SpecialRegExpIPv41.None());
                                        } else {}
                                      }
                                      inlinedVal23 = inlinedVal27;
                                    } else {
                                      inlinedVal23 = (new SpecialRegExpIPv41.None());
                                    }
                                  }
                                }
                                inlinedVal18 = inlinedVal23;
                                break inlinedLbl4
                              }
                              inlinedVal18 = (new SpecialRegExpIPv41.None());
                              break inlinedLbl4;
                            }
                            c183 = s4[0];
                            scrut173 = startsWith_Concat_sp_16(p4, c183);
                            if (scrut173 === true) {
                              tmp353 = derive_Concat_sp_19(p4, c183);
                              tmp363 = runtime.safeCall(s4.slice(1));
                              tmp373 = acc2 + c183;
                              inlinedVal18 = SpecialRegExpIPv41.matchImpl(tmp353, tmp363, tmp373);
                            } else {
                              inlinedVal18 = (new SpecialRegExpIPv41.None());
                            }
                          } else if (p4 instanceof SpecialRegExpIPv41.Altern.class) {
                            c183 = s4[0];
                            scrut173 = startsWith_Altern_sp_2(p4, c183);
                            if (scrut173 === true) {
                              tmp353 = derive_Altern_sp_4(p4, c183);
                              tmp363 = runtime.safeCall(s4.slice(1));
                              tmp373 = acc2 + c183;
                              inlinedVal18 = SpecialRegExpIPv41.matchImpl(tmp353, tmp363, tmp373);
                            } else {
                              inlinedVal18 = (new SpecialRegExpIPv41.None());
                            }
                          } else if (p4 instanceof SpecialRegExpIPv41.Nothing.class) {
                            inlinedVal18 = (new SpecialRegExpIPv41.None());
                          } else {}
                        }
                      }
                      inlinedVal13 = inlinedVal18;
                    } else {
                      inlinedVal13 = (new SpecialRegExpIPv41.None());
                    }
                  }
                }
                inlinedVal8 = inlinedVal13;
                break inlinedLbl1
              }
              inlinedVal8 = (new SpecialRegExpIPv41.None());
              break inlinedLbl1;
            }
            c181 = s2[0];
            scrut171 = startsWith_Concat_sp_16(p2, c181);
            if (scrut171 === true) {
              tmp351 = derive_Concat_sp_19(p2, c181);
              tmp361 = runtime.safeCall(s2.slice(1));
              tmp371 = acc + c181;
              inlinedVal8 = SpecialRegExpIPv41.matchImpl(tmp351, tmp361, tmp371);
            } else {
              inlinedVal8 = (new SpecialRegExpIPv41.None());
            }
          } else if (p2 instanceof SpecialRegExpIPv41.Altern.class) {
            c181 = s2[0];
            scrut171 = startsWith_Altern_sp_2(p2, c181);
            if (scrut171 === true) {
              tmp351 = derive_Altern_sp_4(p2, c181);
              tmp361 = runtime.safeCall(s2.slice(1));
              tmp371 = acc + c181;
              inlinedVal8 = SpecialRegExpIPv41.matchImpl(tmp351, tmp361, tmp371);
            } else {
              inlinedVal8 = (new SpecialRegExpIPv41.None());
            }
          } else if (p2 instanceof SpecialRegExpIPv41.Nothing.class) {
            inlinedVal8 = (new SpecialRegExpIPv41.None());
          } else {}
        }
      }
      inlinedVal = inlinedVal8;
      return inlinedVal
    }
    inlinedVal = (new SpecialRegExpIPv41.None());
    return inlinedVal;
  }
};
mkUnion_SpecialRegExpIPv4_sp_11 = function mkUnion_SpecialRegExpIPv4_sp_11(ls) {
  let x5, xs5, arg_Cons_0_4, arg_Cons_1_4, tmp26, ls1, inlinedVal, x4, arg_Cons_0_41;
  arg_Cons_0_4 = ls.x;
  arg_Cons_1_4 = ls.xs;
  xs5 = arg_Cons_1_4;
  x5 = arg_Cons_0_4;
  ls1 = xs5;
  arg_Cons_0_41 = ls1.x;
  ls1.xs;
  x4 = arg_Cons_0_41;
  inlinedVal = x4;
  tmp26 = inlinedVal;
  return (new SpecialRegExpIPv41.Altern.class(x5, tmp26))
};
mkUnion_SpecialRegExpIPv4_sp_8 = function mkUnion_SpecialRegExpIPv4_sp_8(ls) {
  let x5, xs5, arg_Cons_0_4, arg_Cons_1_4, tmp26, ls1, inlinedVal, x51, xs51, arg_Cons_0_41, arg_Cons_1_41, tmp261, ls2, inlinedVal1, x4, arg_Cons_0_42;
  arg_Cons_0_4 = ls.x;
  arg_Cons_1_4 = ls.xs;
  xs5 = arg_Cons_1_4;
  x5 = arg_Cons_0_4;
  ls1 = xs5;
  arg_Cons_0_41 = ls1.x;
  arg_Cons_1_41 = ls1.xs;
  xs51 = arg_Cons_1_41;
  x51 = arg_Cons_0_41;
  ls2 = xs51;
  arg_Cons_0_42 = ls2.x;
  ls2.xs;
  x4 = arg_Cons_0_42;
  inlinedVal1 = x4;
  tmp261 = inlinedVal1;
  inlinedVal = (new SpecialRegExpIPv41.Altern.class(x51, tmp261));
  tmp26 = inlinedVal;
  return (new SpecialRegExpIPv41.Altern.class(x5, tmp26))
};
derive_Altern_sp_4 = function derive_Altern_sp_4(self, c) {
  let tmp1, tmp2, tmp3, arg$Altern$0$, arg$Concat$0$, arg$Concat$1$, self1, c1, inlinedVal;
  tmp1 = derive_Concat_sp_3(self.p1, c);
  self1 = self.p2;
  c1 = c;
  inlinedLbl: {
    let tmp11, tmp21, tmp31, arg$Altern$0$1, arg$Concat$0$1, arg$Concat$1$1;
    tmp11 = derive_Concat_sp_4(self1.p1, c1);
    tmp21 = derive_Concat_sp_5(self1.p2, c1);
    tmp31 = (new SpecialRegExpIPv41.Altern.class(tmp11, tmp21));
    arg$Altern$0$1 = tmp31.p1;
    if (arg$Altern$0$1 instanceof SpecialRegExpIPv41.Concat.class && (arg$Concat$0$1 = arg$Altern$0$1.p1, arg$Concat$1$1 = arg$Altern$0$1.p2, arg$Concat$0$1 instanceof SpecialRegExpIPv41.In.class) && arg$Concat$1$1 instanceof SpecialRegExpIPv41.In.class) {
      inlinedVal = normalize_Altern_sp_10(tmp31);
      break inlinedLbl
    }
    inlinedVal = normalize_Altern_sp_11(tmp31);
  }
  tmp2 = inlinedVal;
  tmp3 = (new SpecialRegExpIPv41.Altern.class(tmp1, tmp2));
  arg$Altern$0$ = tmp3.p1;
  if (arg$Altern$0$ instanceof SpecialRegExpIPv41.Concat.class) {
    arg$Concat$0$ = arg$Altern$0$.p1;
    arg$Concat$1$ = arg$Altern$0$.p2;
    if (arg$Concat$0$ instanceof SpecialRegExpIPv41.Exact.class) {
      if (arg$Concat$1$ instanceof SpecialRegExpIPv41.In.class) {
        return normalize_Altern_sp_12(tmp3)
      }
      return normalize_Altern_sp_11(tmp3);
    }
    return normalize_Altern_sp_11(tmp3);
  }
  return normalize_Altern_sp_11(tmp3);
};
eq_Altern_sp_1 = function eq_Altern_sp_1(self, other) {
  let tmp6, tmp7;
  if (other instanceof SpecialRegExpIPv41.Altern.class) {
    let self1, inlinedVal, self2, inlinedVal1, p1__, p2__, tmp9, tmp10, p1__1, p2__1, tmp91, tmp101;
    self1 = self;
    p1__1 = runtime.safeCall(self1.p1.flat());
    p2__1 = runtime.safeCall(self1.p2.flat());
    tmp91 = (new SpecialRegExpIPv41.Nil());
    tmp101 = concatUnique_SpecialRegExpIPv4_sp_0(tmp91, p1__1);
    inlinedVal = SpecialRegExpIPv41.concatUnique(tmp101, p2__1);
    tmp6 = inlinedVal;
    self2 = other;
    p1__ = runtime.safeCall(self2.p1.flat());
    p2__ = runtime.safeCall(self2.p2.flat());
    tmp9 = (new SpecialRegExpIPv41.Nil());
    tmp10 = concatUnique_SpecialRegExpIPv4_sp_0(tmp9, p1__);
    inlinedVal1 = SpecialRegExpIPv41.concatUnique(tmp10, p2__);
    tmp7 = inlinedVal1;
    return SpecialRegExpIPv41.lsEq(tmp6, tmp7)
  }
  return false;
};
flat_Altern_sp_18 = function flat_Altern_sp_18(self) {
  let p1__, p2__, tmp10, self1, inlinedVal, self2, inlinedVal1, ys, inlinedVal2, y, arg_Cons_0_1, tmp22, e, inlinedVal3, xs, inlinedVal4;
  self1 = self.p1;
  inlinedVal = (new SpecialRegExpIPv41.Cons.class(self1, SpecialRegExpIPv41.Nil));
  p1__ = inlinedVal;
  self2 = self.p2;
  inlinedVal1 = (new SpecialRegExpIPv41.Cons.class(self2, SpecialRegExpIPv41.Nil));
  p2__ = inlinedVal1;
  (new SpecialRegExpIPv41.Nil());
  ys = p1__;
  arg_Cons_0_1 = ys.x;
  ys.xs;
  y = arg_Cons_0_1;
  e = y;
  inlinedVal3 = (new SpecialRegExpIPv41.Cons.class(e, SpecialRegExpIPv41.Nil));
  tmp22 = inlinedVal3;
  xs = tmp22;
  inlinedVal4 = xs;
  inlinedVal2 = inlinedVal4;
  tmp10 = inlinedVal2;
  return concatUnique_SpecialRegExpIPv4_sp_46(tmp10, p2__)
};
normalize_Altern_sp_10 = function normalize_Altern_sp_10(self) {
  let tmp5, self1, inlinedVal, p1__, p2__, tmp10, self2, inlinedVal1, ys, inlinedVal2, xs, ys1, inlinedVal3, y, arg_Cons_0_1, tmp22, e, inlinedVal4, xs1, inlinedVal5, y1, ys__, arg_Cons_0_11, arg_Cons_1_1, tmp221;
  self1 = self;
  self2 = self1.p1;
  inlinedVal1 = (new SpecialRegExpIPv41.Cons.class(self2, SpecialRegExpIPv41.Nil));
  p1__ = inlinedVal1;
  p2__ = runtime.safeCall(self1.p2.flat());
  (new SpecialRegExpIPv41.Nil());
  ys = p1__;
  arg_Cons_0_1 = ys.x;
  ys.xs;
  y = arg_Cons_0_1;
  e = y;
  inlinedVal4 = (new SpecialRegExpIPv41.Cons.class(e, SpecialRegExpIPv41.Nil));
  tmp22 = inlinedVal4;
  xs1 = tmp22;
  inlinedVal5 = xs1;
  inlinedVal2 = inlinedVal5;
  tmp10 = inlinedVal2;
  xs = tmp10;
  ys1 = p2__;
  if (ys1 instanceof SpecialRegExpIPv41.Cons.class) {
    let ls, e1, inlinedVal6, x2, scrut10, arg_Cons_0_, tmp21, self3, other, inlinedVal7, p2__1, p1__3, scrut6, scrut7, arg_Concat_0_, arg_Concat_1_;
    arg_Cons_0_11 = ys1.x;
    arg_Cons_1_1 = ys1.xs;
    ys__ = arg_Cons_1_1;
    y1 = arg_Cons_0_11;
    ls = xs;
    e1 = y1;
    arg_Cons_0_ = ls.x;
    ls.xs;
    x2 = arg_Cons_0_;
    self3 = x2;
    other = e1;
    if (other instanceof SpecialRegExpIPv41.Concat.class) {
      let self4, other1, inlinedVal8, chars__1, arg_In_0_;
      arg_Concat_0_ = other.p1;
      arg_Concat_1_ = other.p2;
      p2__1 = arg_Concat_1_;
      p1__3 = arg_Concat_0_;
      self4 = self3.p1;
      other1 = p1__3;
      if (other1 instanceof SpecialRegExpIPv41.In.class) {
        let arr1, arr2, inlinedVal9;
        arg_In_0_ = other1.chars;
        chars__1 = arg_In_0_;
        arr1 = self4.chars;
        arr2 = chars__1;
        inlinedVal9 = runtime.safeCall(SpecialRegExpIPv4__Legacy["SeqHelper$SpecialRegExpIPv4"].eq(arr1, arr2));
        inlinedVal8 = inlinedVal9;
      } else {
        inlinedVal8 = false;
      }
      scrut6 = inlinedVal8;
      if (scrut6 === true) {
        let self5, other2, inlinedVal9, chars__11, arg_In_0_1;
        self5 = self3.p2;
        other2 = p2__1;
        if (other2 instanceof SpecialRegExpIPv41.In.class) {
          let arr1, arr2, inlinedVal10;
          arg_In_0_1 = other2.chars;
          chars__11 = arg_In_0_1;
          arr1 = self5.chars;
          arr2 = chars__11;
          inlinedVal10 = runtime.safeCall(SpecialRegExpIPv4__Legacy["SeqHelper$SpecialRegExpIPv4"].eq(arr1, arr2));
          inlinedVal9 = inlinedVal10;
        } else {
          inlinedVal9 = false;
        }
        scrut7 = inlinedVal9;
        if (scrut7 === true) {
          inlinedVal7 = true;
        } else {
          inlinedVal7 = false;
        }
      } else {
        inlinedVal7 = false;
      }
    } else {
      inlinedVal7 = false;
    }
    scrut10 = inlinedVal7;
    if (scrut10 === true) {
      inlinedVal6 = ls;
    } else {
      let e2, inlinedVal8;
      e2 = e1;
      inlinedVal8 = (new SpecialRegExpIPv41.Cons.class(e2, SpecialRegExpIPv41.Nil));
      tmp21 = inlinedVal8;
      inlinedVal6 = (new SpecialRegExpIPv41.Cons.class(x2, tmp21));
    }
    tmp221 = inlinedVal6;
    inlinedVal3 = SpecialRegExpIPv41.concatUnique(tmp221, ys__);
  } else {
    inlinedVal3 = xs;
  }
  inlinedVal = inlinedVal3;
  tmp5 = inlinedVal;
  return SpecialRegExpIPv41.mkUnion(tmp5)
};
normalize_Altern_sp_11 = function normalize_Altern_sp_11(self) {
  let tmp5, self1, inlinedVal, p2__, tmp9, tmp10, xs, inlinedVal1;
  self1 = self;
  self1.p1;
  (new SpecialRegExpIPv41.Nil());
  p2__ = runtime.safeCall(self1.p2.flat());
  tmp9 = (new SpecialRegExpIPv41.Nil());
  xs = tmp9;
  inlinedVal1 = xs;
  tmp10 = inlinedVal1;
  inlinedVal = concatUnique_SpecialRegExpIPv4_sp_0(tmp10, p2__);
  tmp5 = inlinedVal;
  return SpecialRegExpIPv41.mkUnion(tmp5)
};
normalize_Altern_sp_12 = function normalize_Altern_sp_12(self) {
  let tmp5, self1, inlinedVal, p1__, p2__, tmp10, self2, inlinedVal1, ys, inlinedVal2, xs, ys1, inlinedVal3, y, arg_Cons_0_1, tmp22, e, inlinedVal4, xs1, inlinedVal5, y1, ys__, arg_Cons_0_11, arg_Cons_1_1, tmp221;
  self1 = self;
  self2 = self1.p1;
  inlinedVal1 = (new SpecialRegExpIPv41.Cons.class(self2, SpecialRegExpIPv41.Nil));
  p1__ = inlinedVal1;
  p2__ = runtime.safeCall(self1.p2.flat());
  (new SpecialRegExpIPv41.Nil());
  ys = p1__;
  arg_Cons_0_1 = ys.x;
  ys.xs;
  y = arg_Cons_0_1;
  e = y;
  inlinedVal4 = (new SpecialRegExpIPv41.Cons.class(e, SpecialRegExpIPv41.Nil));
  tmp22 = inlinedVal4;
  xs1 = tmp22;
  inlinedVal5 = xs1;
  inlinedVal2 = inlinedVal5;
  tmp10 = inlinedVal2;
  xs = tmp10;
  ys1 = p2__;
  if (ys1 instanceof SpecialRegExpIPv41.Cons.class) {
    let ls, e1, inlinedVal6, x2, scrut10, arg_Cons_0_, tmp21, self3, other, inlinedVal7, p2__1, p1__3, scrut6, scrut7, arg_Concat_0_, arg_Concat_1_;
    arg_Cons_0_11 = ys1.x;
    arg_Cons_1_1 = ys1.xs;
    ys__ = arg_Cons_1_1;
    y1 = arg_Cons_0_11;
    ls = xs;
    e1 = y1;
    arg_Cons_0_ = ls.x;
    ls.xs;
    x2 = arg_Cons_0_;
    self3 = x2;
    other = e1;
    if (other instanceof SpecialRegExpIPv41.Concat.class) {
      let other1, inlinedVal8, ch__, arg_Exact_0_;
      arg_Concat_0_ = other.p1;
      arg_Concat_1_ = other.p2;
      p2__1 = arg_Concat_1_;
      p1__3 = arg_Concat_0_;
      self3.p1;
      other1 = p1__3;
      if (other1 instanceof SpecialRegExpIPv41.Exact.class) {
        arg_Exact_0_ = other1.ch;
        ch__ = arg_Exact_0_;
        inlinedVal8 = "5" == ch__;
      } else {
        inlinedVal8 = false;
      }
      scrut6 = inlinedVal8;
      if (scrut6 === true) {
        let self4, other2, inlinedVal9, chars__1, arg_In_0_;
        self4 = self3.p2;
        other2 = p2__1;
        if (other2 instanceof SpecialRegExpIPv41.In.class) {
          let arr1, arr2, inlinedVal10;
          arg_In_0_ = other2.chars;
          chars__1 = arg_In_0_;
          arr1 = self4.chars;
          arr2 = chars__1;
          inlinedVal10 = runtime.safeCall(SpecialRegExpIPv4__Legacy["SeqHelper$SpecialRegExpIPv4"].eq(arr1, arr2));
          inlinedVal9 = inlinedVal10;
        } else {
          inlinedVal9 = false;
        }
        scrut7 = inlinedVal9;
        if (scrut7 === true) {
          inlinedVal7 = true;
        } else {
          inlinedVal7 = false;
        }
      } else {
        inlinedVal7 = false;
      }
    } else {
      inlinedVal7 = false;
    }
    scrut10 = inlinedVal7;
    if (scrut10 === true) {
      inlinedVal6 = ls;
    } else {
      let e2, inlinedVal8;
      e2 = e1;
      inlinedVal8 = (new SpecialRegExpIPv41.Cons.class(e2, SpecialRegExpIPv41.Nil));
      tmp21 = inlinedVal8;
      inlinedVal6 = (new SpecialRegExpIPv41.Cons.class(x2, tmp21));
    }
    tmp221 = inlinedVal6;
    inlinedVal3 = SpecialRegExpIPv41.concatUnique(tmp221, ys__);
  } else {
    inlinedVal3 = xs;
  }
  inlinedVal = inlinedVal3;
  tmp5 = inlinedVal;
  return SpecialRegExpIPv41.mkUnion(tmp5)
};
normalize_Altern_sp_13 = function normalize_Altern_sp_13(self) {
  let tmp5, self1, inlinedVal, p1__, p2__, tmp10, self2, inlinedVal1, self3, inlinedVal2, p1__1, p2__1, tmp101, self4, inlinedVal3, self5, inlinedVal4, xs, ys, inlinedVal5, y, arg_Cons_0_1, tmp22, xs1, inlinedVal6, ys1, inlinedVal7, ls, e, inlinedVal8, x2, arg_Cons_0_, tmp21, e1, inlinedVal9, ys2, inlinedVal10, y1, arg_Cons_0_11, tmp221, e2, inlinedVal11, xs2, inlinedVal12, y2, arg_Cons_0_12, tmp222, e3, inlinedVal13, xs3, inlinedVal14;
  self1 = self;
  self2 = self1.p1;
  inlinedVal1 = (new SpecialRegExpIPv41.Cons.class(self2, SpecialRegExpIPv41.Nil));
  p1__ = inlinedVal1;
  self3 = self1.p2;
  self4 = self3.p1;
  inlinedVal3 = (new SpecialRegExpIPv41.Cons.class(self4, SpecialRegExpIPv41.Nil));
  p1__1 = inlinedVal3;
  self5 = self3.p2;
  inlinedVal4 = (new SpecialRegExpIPv41.Cons.class(self5, SpecialRegExpIPv41.Nil));
  p2__1 = inlinedVal4;
  (new SpecialRegExpIPv41.Nil());
  ys1 = p1__1;
  arg_Cons_0_12 = ys1.x;
  ys1.xs;
  y2 = arg_Cons_0_12;
  e3 = y2;
  inlinedVal13 = (new SpecialRegExpIPv41.Cons.class(e3, SpecialRegExpIPv41.Nil));
  tmp222 = inlinedVal13;
  xs3 = tmp222;
  inlinedVal14 = xs3;
  inlinedVal7 = inlinedVal14;
  tmp101 = inlinedVal7;
  xs = tmp101;
  ys = p2__1;
  arg_Cons_0_1 = ys.x;
  ys.xs;
  y = arg_Cons_0_1;
  ls = xs;
  e = y;
  arg_Cons_0_ = ls.x;
  ls.xs;
  x2 = arg_Cons_0_;
  e1 = e;
  inlinedVal9 = (new SpecialRegExpIPv41.Cons.class(e1, SpecialRegExpIPv41.Nil));
  tmp21 = inlinedVal9;
  inlinedVal8 = (new SpecialRegExpIPv41.Cons.class(x2, tmp21));
  tmp22 = inlinedVal8;
  xs1 = tmp22;
  inlinedVal6 = xs1;
  inlinedVal5 = inlinedVal6;
  inlinedVal2 = inlinedVal5;
  p2__ = inlinedVal2;
  (new SpecialRegExpIPv41.Nil());
  ys2 = p1__;
  arg_Cons_0_11 = ys2.x;
  ys2.xs;
  y1 = arg_Cons_0_11;
  e2 = y1;
  inlinedVal11 = (new SpecialRegExpIPv41.Cons.class(e2, SpecialRegExpIPv41.Nil));
  tmp221 = inlinedVal11;
  xs2 = tmp221;
  inlinedVal12 = xs2;
  inlinedVal10 = inlinedVal12;
  tmp10 = inlinedVal10;
  inlinedVal = concatUnique_SpecialRegExpIPv4_sp_41(tmp10, p2__);
  tmp5 = inlinedVal;
  return mkUnion_SpecialRegExpIPv4_sp_8(tmp5)
};
normalize_Altern_sp_14 = function normalize_Altern_sp_14(self) {
  let tmp5, self1, inlinedVal, p1__, p2__, tmp10, self2, inlinedVal1, self3, inlinedVal2, p1__1, p2__1, tmp101, self4, inlinedVal3, self5, inlinedVal4, xs, ys, inlinedVal5, y, arg_Cons_0_1, tmp22, xs1, inlinedVal6, ys1, inlinedVal7, ls, e, inlinedVal8, x2, arg_Cons_0_, tmp21, e1, inlinedVal9, ys2, inlinedVal10, y1, arg_Cons_0_11, tmp221, e2, inlinedVal11, xs2, inlinedVal12, y2, arg_Cons_0_12, tmp222, e3, inlinedVal13, xs3, inlinedVal14;
  self1 = self;
  self2 = self1.p1;
  inlinedVal1 = (new SpecialRegExpIPv41.Cons.class(self2, SpecialRegExpIPv41.Nil));
  p1__ = inlinedVal1;
  self3 = self1.p2;
  self4 = self3.p1;
  inlinedVal3 = (new SpecialRegExpIPv41.Cons.class(self4, SpecialRegExpIPv41.Nil));
  p1__1 = inlinedVal3;
  self5 = self3.p2;
  inlinedVal4 = (new SpecialRegExpIPv41.Cons.class(self5, SpecialRegExpIPv41.Nil));
  p2__1 = inlinedVal4;
  (new SpecialRegExpIPv41.Nil());
  ys1 = p1__1;
  arg_Cons_0_12 = ys1.x;
  ys1.xs;
  y2 = arg_Cons_0_12;
  e3 = y2;
  inlinedVal13 = (new SpecialRegExpIPv41.Cons.class(e3, SpecialRegExpIPv41.Nil));
  tmp222 = inlinedVal13;
  xs3 = tmp222;
  inlinedVal14 = xs3;
  inlinedVal7 = inlinedVal14;
  tmp101 = inlinedVal7;
  xs = tmp101;
  ys = p2__1;
  arg_Cons_0_1 = ys.x;
  ys.xs;
  y = arg_Cons_0_1;
  ls = xs;
  e = y;
  arg_Cons_0_ = ls.x;
  ls.xs;
  x2 = arg_Cons_0_;
  e1 = e;
  inlinedVal9 = (new SpecialRegExpIPv41.Cons.class(e1, SpecialRegExpIPv41.Nil));
  tmp21 = inlinedVal9;
  inlinedVal8 = (new SpecialRegExpIPv41.Cons.class(x2, tmp21));
  tmp22 = inlinedVal8;
  xs1 = tmp22;
  inlinedVal6 = xs1;
  inlinedVal5 = inlinedVal6;
  inlinedVal2 = inlinedVal5;
  p2__ = inlinedVal2;
  (new SpecialRegExpIPv41.Nil());
  ys2 = p1__;
  arg_Cons_0_11 = ys2.x;
  ys2.xs;
  y1 = arg_Cons_0_11;
  e2 = y1;
  inlinedVal11 = (new SpecialRegExpIPv41.Cons.class(e2, SpecialRegExpIPv41.Nil));
  tmp221 = inlinedVal11;
  xs2 = tmp221;
  inlinedVal12 = xs2;
  inlinedVal10 = inlinedVal12;
  tmp10 = inlinedVal10;
  inlinedVal = concatUnique_SpecialRegExpIPv4_sp_41(tmp10, p2__);
  tmp5 = inlinedVal;
  return mkUnion_SpecialRegExpIPv4_sp_8(tmp5)
};
normalize_Altern_sp_16 = function normalize_Altern_sp_16(self) {
  let tmp5, self1, inlinedVal, ls, inlinedVal1, x4, arg_Cons_0_4, p1__, tmp10, self2, inlinedVal2, xs, inlinedVal3, ys, inlinedVal4, y, arg_Cons_0_1, tmp22, e, inlinedVal5, xs1, inlinedVal6;
  self1 = self;
  self2 = self1.p1;
  inlinedVal2 = (new SpecialRegExpIPv41.Cons.class(self2, SpecialRegExpIPv41.Nil));
  p1__ = inlinedVal2;
  self1.p2;
  (new SpecialRegExpIPv41.Nil());
  (new SpecialRegExpIPv41.Nil());
  ys = p1__;
  arg_Cons_0_1 = ys.x;
  ys.xs;
  y = arg_Cons_0_1;
  e = y;
  inlinedVal5 = (new SpecialRegExpIPv41.Cons.class(e, SpecialRegExpIPv41.Nil));
  tmp22 = inlinedVal5;
  xs1 = tmp22;
  inlinedVal6 = xs1;
  inlinedVal4 = inlinedVal6;
  tmp10 = inlinedVal4;
  xs = tmp10;
  inlinedVal3 = xs;
  inlinedVal = inlinedVal3;
  tmp5 = inlinedVal;
  ls = tmp5;
  arg_Cons_0_4 = ls.x;
  ls.xs;
  x4 = arg_Cons_0_4;
  inlinedVal1 = x4;
  return inlinedVal1
};
normalize_Altern_sp_2 = function normalize_Altern_sp_2(self) {
  let tmp5, self1, inlinedVal, ls, inlinedVal1, x4, arg_Cons_0_4, p1__, tmp10, self2, inlinedVal2, ys, inlinedVal3, xs, inlinedVal4, y, arg_Cons_0_1, tmp22, e, inlinedVal5, xs1, inlinedVal6;
  self1 = self;
  self2 = self1.p1;
  inlinedVal2 = (new SpecialRegExpIPv41.Cons.class(self2, SpecialRegExpIPv41.Nil));
  p1__ = inlinedVal2;
  self1.p2;
  (new SpecialRegExpIPv41.Nil());
  (new SpecialRegExpIPv41.Nil());
  ys = p1__;
  arg_Cons_0_1 = ys.x;
  ys.xs;
  y = arg_Cons_0_1;
  e = y;
  inlinedVal5 = (new SpecialRegExpIPv41.Cons.class(e, SpecialRegExpIPv41.Nil));
  tmp22 = inlinedVal5;
  xs1 = tmp22;
  inlinedVal6 = xs1;
  inlinedVal3 = inlinedVal6;
  tmp10 = inlinedVal3;
  xs = tmp10;
  inlinedVal4 = xs;
  inlinedVal = inlinedVal4;
  tmp5 = inlinedVal;
  ls = tmp5;
  arg_Cons_0_4 = ls.x;
  ls.xs;
  x4 = arg_Cons_0_4;
  inlinedVal1 = x4;
  return inlinedVal1
};
startsWith_Altern_sp_2 = function startsWith_Altern_sp_2(self, c) {
  let tmp8, self1, c1, inlinedVal, tmp16, c2, inlinedVal1;
  self1 = self.p1;
  c1 = c;
  self1.p1;
  c2 = c1;
  inlinedVal1 = "2" == c2;
  tmp16 = inlinedVal1;
  if (tmp16 === false) {
    self1.p1;
    inlinedVal = false;
  } else {
    inlinedVal = true;
  }
  tmp8 = inlinedVal;
  if (tmp8 === false) {
    let self2, c3, inlinedVal2, tmp81;
    self2 = self.p2;
    c3 = c;
    tmp81 = runtime.safeCall(self2.p1.startsWith(c3));
    if (tmp81 === false) {
      inlinedVal2 = runtime.safeCall(self2.p2.startsWith(c3));
      return inlinedVal2
    }
    inlinedVal2 = true;
    return inlinedVal2;
  }
  return true;
};
derive_In_sp_0 = function derive_In_sp_0(self, c) {
  let scrut2, self1, c1, inlinedVal, arr, ele, inlinedVal1;
  self1 = self;
  c1 = c;
  arr = self1.chars;
  ele = c1;
  inlinedVal1 = runtime.safeCall(SpecialRegExpIPv4__Legacy["SeqHelper$SpecialRegExpIPv4"].has(arr, ele));
  inlinedVal = inlinedVal1;
  scrut2 = inlinedVal;
  if (scrut2 === true) {
    return (new SpecialRegExpIPv41.Empty.class())
  }
  return (new SpecialRegExpIPv41.Nothing.class());
};
derive_Concat_sp_15 = function derive_Concat_sp_15(self, c) {
  let p1__1, tmp14, arg$Concat$0$, c1, inlinedVal, self1, inlinedVal1, scrut, c2, inlinedVal2;
  self.p1;
  c1 = c;
  c2 = c1;
  inlinedVal2 = "." == c2;
  scrut = inlinedVal2;
  if (scrut === true) {
    inlinedVal = (new SpecialRegExpIPv41.Empty.class());
  } else {
    inlinedVal = (new SpecialRegExpIPv41.Nothing.class());
  }
  p1__1 = inlinedVal;
  self.p1;
  tmp14 = (new SpecialRegExpIPv41.Concat.class(p1__1, self.p2));
  arg$Concat$0$ = tmp14.p1;
  if (arg$Concat$0$ instanceof SpecialRegExpIPv41.Empty.class) {
    let self2, inlinedVal3;
    self2 = tmp14;
    self2.p1;
    inlinedVal3 = normalize_Altern_sp_14(self2.p2);
    return inlinedVal3
  }
  {
    let p1__2, self2, inlinedVal3;
    self1 = tmp14;
    self2 = self1.p1;
    inlinedVal3 = self2;
    p1__2 = inlinedVal3;
    inlinedVal1 = p1__2;
    return inlinedVal1;
  }
};
derive_Concat_sp_16 = function derive_Concat_sp_16(self, c) {
  let p1__1, tmp14, arg$Concat$0$, arg$Concat$1$, arg$Altern$0$, arg$Altern$1$, arg$Concat$1$1;
  p1__1 = derive_Concat_sp_17(self.p1, c);
  self.p1;
  tmp14 = (new SpecialRegExpIPv41.Concat.class(p1__1, self.p2));
  arg$Concat$0$ = tmp14.p1;
  if (arg$Concat$0$ instanceof SpecialRegExpIPv41.Altern.class && (arg$Altern$0$ = arg$Concat$0$.p1, arg$Altern$1$ = arg$Concat$0$.p2, arg$Altern$0$ instanceof SpecialRegExpIPv41.Concat.class) && (arg$Concat$1$1 = arg$Altern$0$.p2, arg$Concat$1$1 instanceof SpecialRegExpIPv41.Exact.class) && arg$Altern$1$ instanceof SpecialRegExpIPv41.Empty.class) {
    let self1, inlinedVal, p1__2, tmp15, self2, inlinedVal1, tmp5;
    self1 = tmp14;
    self2 = self1.p1;
    tmp5 = flat_Altern_sp_18(self2);
    inlinedVal1 = mkUnion_SpecialRegExpIPv4_sp_11(tmp5);
    p1__2 = inlinedVal1;
    tmp15 = normalize_Altern_sp_14(self1.p2);
    inlinedVal = (new SpecialRegExpIPv41.Concat.class(p1__2, tmp15));
    return inlinedVal
  }
  if (arg$Concat$0$ instanceof SpecialRegExpIPv41.Concat.class) {
    let self1, inlinedVal;
    arg$Concat$1$ = arg$Concat$0$.p2;
    if (arg$Concat$1$ instanceof SpecialRegExpIPv41.Exact.class) {
      let self2, inlinedVal1, p1__2, tmp15;
      self2 = tmp14;
      p1__2 = normalize_Concat_sp_8(self2.p1);
      if (p1__2 instanceof SpecialRegExpIPv41.Nothing.class) {
        inlinedVal1 = p1__2;
        return inlinedVal1
      }
      tmp15 = normalize_Altern_sp_14(self2.p2);
      inlinedVal1 = (new SpecialRegExpIPv41.Concat.class(p1__2, tmp15));
      return inlinedVal1;
    }
    {
      let p1__2, tmp15;
      self1 = tmp14;
      p1__2 = normalize_Concat_sp_8(self1.p1);
      if (p1__2 instanceof SpecialRegExpIPv41.Nothing.class) {
        inlinedVal = p1__2;
        return inlinedVal
      }
      tmp15 = normalize_Altern_sp_14(self1.p2);
      inlinedVal = (new SpecialRegExpIPv41.Concat.class(p1__2, tmp15));
      return inlinedVal;
    }
  } else if (arg$Concat$0$ instanceof SpecialRegExpIPv41.Exact.class) {
    let self1, inlinedVal, p1__2, tmp15, self2, inlinedVal1;
    self1 = tmp14;
    self2 = self1.p1;
    inlinedVal1 = self2;
    p1__2 = inlinedVal1;
    tmp15 = normalize_Altern_sp_14(self1.p2);
    inlinedVal = (new SpecialRegExpIPv41.Concat.class(p1__2, tmp15));
    return inlinedVal
  } else if (arg$Concat$0$ instanceof SpecialRegExpIPv41.Nothing.class) {
    let self1, inlinedVal, p1__2, self2, inlinedVal1;
    self1 = tmp14;
    self2 = self1.p1;
    inlinedVal1 = self2;
    p1__2 = inlinedVal1;
    inlinedVal = p1__2;
    return inlinedVal
  }
};
derive_Concat_sp_17 = function derive_Concat_sp_17(self, c) {
  let p1__1, scrut3, tmp11, tmp12, tmp13, tmp14, arg$Altern$1$;
  p1__1 = runtime.safeCall(self.p1.derive(c));
  scrut3 = runtime.safeCall(self.p1.canBeEmpty());
  if (scrut3 === true) {
    let c1, inlinedVal, scrut, c2, inlinedVal1;
    tmp11 = (new SpecialRegExpIPv41.Concat.class(p1__1, self.p2));
    self.p2;
    c1 = c;
    c2 = c1;
    inlinedVal1 = "." == c2;
    scrut = inlinedVal1;
    if (scrut === true) {
      inlinedVal = (new SpecialRegExpIPv41.Empty.class());
    } else {
      inlinedVal = (new SpecialRegExpIPv41.Nothing.class());
    }
    tmp12 = inlinedVal;
    tmp13 = (new SpecialRegExpIPv41.Altern.class(tmp11, tmp12));
    arg$Altern$1$ = tmp13.p2;
    if (arg$Altern$1$ instanceof SpecialRegExpIPv41.Empty.class) {
      let self1, inlinedVal2, tmp5;
      self1 = tmp13;
      tmp5 = flat_Altern_sp_18(self1);
      inlinedVal2 = mkUnion_SpecialRegExpIPv4_sp_11(tmp5);
      return inlinedVal2
    }
    return normalize_Altern_sp_16(tmp13);
  }
  tmp14 = (new SpecialRegExpIPv41.Concat.class(p1__1, self.p2));
  return normalize_Concat_sp_8(tmp14);
};
derive_Concat_sp_19 = function derive_Concat_sp_19(self, c) {
  let p1__1, scrut3, tmp11, tmp12, tmp13, tmp14;
  p1__1 = runtime.safeCall(self.p1.derive(c));
  scrut3 = runtime.safeCall(self.p1.canBeEmpty());
  if (scrut3 === true) {
    let self1, inlinedVal, tmp5, self2, inlinedVal1, p1__, p2__, tmp10, self3, inlinedVal2, ys, inlinedVal3, xs, ys1, inlinedVal4, y, arg_Cons_0_1, tmp22, e, inlinedVal5, xs1, inlinedVal6, y1, ys__, arg_Cons_0_11, arg_Cons_1_1, tmp221;
    tmp11 = (new SpecialRegExpIPv41.Concat.class(p1__1, self.p2));
    tmp12 = derive_Altern_sp_4(self.p2, c);
    tmp13 = (new SpecialRegExpIPv41.Altern.class(tmp11, tmp12));
    self1 = tmp13;
    self2 = self1;
    self3 = self2.p1;
    inlinedVal2 = (new SpecialRegExpIPv41.Cons.class(self3, SpecialRegExpIPv41.Nil));
    p1__ = inlinedVal2;
    p2__ = runtime.safeCall(self2.p2.flat());
    (new SpecialRegExpIPv41.Nil());
    ys = p1__;
    arg_Cons_0_1 = ys.x;
    ys.xs;
    y = arg_Cons_0_1;
    e = y;
    inlinedVal5 = (new SpecialRegExpIPv41.Cons.class(e, SpecialRegExpIPv41.Nil));
    tmp22 = inlinedVal5;
    xs1 = tmp22;
    inlinedVal6 = xs1;
    inlinedVal3 = inlinedVal6;
    tmp10 = inlinedVal3;
    xs = tmp10;
    ys1 = p2__;
    if (ys1 instanceof SpecialRegExpIPv41.Cons.class) {
      let ls, e1, inlinedVal7, x2, scrut10, arg_Cons_0_, tmp21, self4, other, inlinedVal8, p2__1, p1__3, scrut6, scrut7, arg_Concat_0_, arg_Concat_1_;
      arg_Cons_0_11 = ys1.x;
      arg_Cons_1_1 = ys1.xs;
      ys__ = arg_Cons_1_1;
      y1 = arg_Cons_0_11;
      ls = xs;
      e1 = y1;
      arg_Cons_0_ = ls.x;
      ls.xs;
      x2 = arg_Cons_0_;
      self4 = x2;
      other = e1;
      if (other instanceof SpecialRegExpIPv41.Concat.class) {
        arg_Concat_0_ = other.p1;
        arg_Concat_1_ = other.p2;
        p2__1 = arg_Concat_1_;
        p1__3 = arg_Concat_0_;
        scrut6 = runtime.safeCall(self4.p1.eq(p1__3));
        if (scrut6 === true) {
          scrut7 = eq_Altern_sp_1(self4.p2, p2__1);
          if (scrut7 === true) {
            inlinedVal8 = true;
          } else {
            inlinedVal8 = false;
          }
        } else {
          inlinedVal8 = false;
        }
      } else {
        inlinedVal8 = false;
      }
      scrut10 = inlinedVal8;
      if (scrut10 === true) {
        inlinedVal7 = ls;
      } else {
        let e2, inlinedVal9;
        e2 = e1;
        inlinedVal9 = (new SpecialRegExpIPv41.Cons.class(e2, SpecialRegExpIPv41.Nil));
        tmp21 = inlinedVal9;
        inlinedVal7 = (new SpecialRegExpIPv41.Cons.class(x2, tmp21));
      }
      tmp221 = inlinedVal7;
      inlinedVal4 = SpecialRegExpIPv41.concatUnique(tmp221, ys__);
    } else {
      inlinedVal4 = xs;
    }
    inlinedVal1 = inlinedVal4;
    tmp5 = inlinedVal1;
    inlinedVal = SpecialRegExpIPv41.mkUnion(tmp5);
    return inlinedVal
  }
  {
    let self1, inlinedVal, p1__2, tmp15;
    tmp14 = (new SpecialRegExpIPv41.Concat.class(p1__1, self.p2));
    self1 = tmp14;
    p1__2 = runtime.safeCall(self1.p1.normalize());
    if (p1__2 instanceof SpecialRegExpIPv41.Empty.class) {
      inlinedVal = normalize_Altern_sp_14(self1.p2);
      return inlinedVal
    } else if (p1__2 instanceof SpecialRegExpIPv41.Nothing.class) {
      inlinedVal = p1__2;
      return inlinedVal
    }
    tmp15 = normalize_Altern_sp_14(self1.p2);
    inlinedVal = (new SpecialRegExpIPv41.Concat.class(p1__2, tmp15));
    return inlinedVal;
  }
};
derive_Concat_sp_3 = function derive_Concat_sp_3(self, c) {
  let p1__1, tmp14, arg$Concat$0$, c1, inlinedVal, self1, inlinedVal1, scrut, c2, inlinedVal2;
  self.p1;
  c1 = c;
  c2 = c1;
  inlinedVal2 = "2" == c2;
  scrut = inlinedVal2;
  if (scrut === true) {
    inlinedVal = (new SpecialRegExpIPv41.Empty.class());
  } else {
    inlinedVal = (new SpecialRegExpIPv41.Nothing.class());
  }
  p1__1 = inlinedVal;
  self.p1;
  tmp14 = (new SpecialRegExpIPv41.Concat.class(p1__1, self.p2));
  arg$Concat$0$ = tmp14.p1;
  if (arg$Concat$0$ instanceof SpecialRegExpIPv41.Empty.class) {
    let self2, inlinedVal3, self3, inlinedVal4, p1__2, tmp15, self4, inlinedVal5, self5, inlinedVal6;
    self2 = tmp14;
    self2.p1;
    self3 = self2.p2;
    self4 = self3.p1;
    inlinedVal5 = self4;
    p1__2 = inlinedVal5;
    self5 = self3.p2;
    inlinedVal6 = self5;
    tmp15 = inlinedVal6;
    inlinedVal4 = (new SpecialRegExpIPv41.Concat.class(p1__2, tmp15));
    inlinedVal3 = inlinedVal4;
    return inlinedVal3
  }
  {
    let p1__2, self2, inlinedVal3;
    self1 = tmp14;
    self2 = self1.p1;
    inlinedVal3 = self2;
    p1__2 = inlinedVal3;
    inlinedVal1 = p1__2;
    return inlinedVal1;
  }
};
derive_Concat_sp_4 = function derive_Concat_sp_4(self, c) {
  let p1__1, tmp14, arg$Concat$0$, c1, inlinedVal, self1, inlinedVal1, scrut, c2, inlinedVal2;
  self.p1;
  c1 = c;
  c2 = c1;
  inlinedVal2 = "2" == c2;
  scrut = inlinedVal2;
  if (scrut === true) {
    inlinedVal = (new SpecialRegExpIPv41.Empty.class());
  } else {
    inlinedVal = (new SpecialRegExpIPv41.Nothing.class());
  }
  p1__1 = inlinedVal;
  self.p1;
  tmp14 = (new SpecialRegExpIPv41.Concat.class(p1__1, self.p2));
  arg$Concat$0$ = tmp14.p1;
  if (arg$Concat$0$ instanceof SpecialRegExpIPv41.Empty.class) {
    let self2, inlinedVal3, self3, inlinedVal4, p1__2, tmp15, self4, inlinedVal5, self5, inlinedVal6;
    self2 = tmp14;
    self2.p1;
    self3 = self2.p2;
    self4 = self3.p1;
    inlinedVal5 = self4;
    p1__2 = inlinedVal5;
    self5 = self3.p2;
    inlinedVal6 = self5;
    tmp15 = inlinedVal6;
    inlinedVal4 = (new SpecialRegExpIPv41.Concat.class(p1__2, tmp15));
    inlinedVal3 = inlinedVal4;
    return inlinedVal3
  }
  {
    let p1__2, self2, inlinedVal3;
    self1 = tmp14;
    self2 = self1.p1;
    inlinedVal3 = self2;
    p1__2 = inlinedVal3;
    inlinedVal1 = p1__2;
    return inlinedVal1;
  }
};
derive_Concat_sp_5 = function derive_Concat_sp_5(self, c) {
  let p1__1, tmp11, tmp12, tmp13, arg$Altern$1$, arg$Concat$0$, arg$Concat$1$, arg$Altern$0$, arg$Altern$1$1, arg$Concat$0$1, arg$Concat$1$1, self1, c1, inlinedVal, self2, c2, inlinedVal1, p1__11, tmp111, tmp121, tmp131, arg$Altern$1$2, self3, c3, inlinedVal2, self4, inlinedVal3, tmp1, tmp2, tmp3, arg$Altern$0$1, inlinedVal4, tmp14, tmp21, tmp31, arg$Altern$0$2, c4, inlinedVal5, inlinedVal6, scrut, c5, inlinedVal7;
  self1 = self.p1;
  c1 = c;
  self1.p1;
  c4 = c1;
  c5 = c4;
  inlinedVal7 = "1" == c5;
  scrut = inlinedVal7;
  if (scrut === true) {
    inlinedVal5 = (new SpecialRegExpIPv41.Empty.class());
  } else {
    inlinedVal5 = (new SpecialRegExpIPv41.Nothing.class());
  }
  tmp14 = inlinedVal5;
  self1.p2;
  inlinedVal6 = (new SpecialRegExpIPv41.Nothing.class());
  tmp21 = inlinedVal6;
  tmp31 = (new SpecialRegExpIPv41.Altern.class(tmp14, tmp21));
  arg$Altern$0$2 = tmp31.p1;
  if (arg$Altern$0$2 instanceof SpecialRegExpIPv41.Empty.class) {
    inlinedVal = normalize_Altern_sp_2(tmp31);
  } else {
    let self5, inlinedVal8, self6, inlinedVal9;
    self5 = tmp31;
    self6 = self5;
    self6.p1;
    (new SpecialRegExpIPv41.Nil());
    self6.p2;
    (new SpecialRegExpIPv41.Nil());
    (new SpecialRegExpIPv41.Nil());
    inlinedVal9 = (new SpecialRegExpIPv41.Nothing.class());
    inlinedVal8 = inlinedVal9;
    inlinedVal = inlinedVal8;
  }
  p1__1 = inlinedVal;
  self.p1;
  tmp11 = (new SpecialRegExpIPv41.Concat.class(p1__1, self.p2));
  self2 = self.p2;
  c2 = c;
  self3 = self2.p1;
  c3 = c2;
  tmp1 = derive_In_sp_0(self3.p1, c3);
  self3.p2;
  inlinedVal4 = (new SpecialRegExpIPv41.Nothing.class());
  tmp2 = inlinedVal4;
  tmp3 = (new SpecialRegExpIPv41.Altern.class(tmp1, tmp2));
  arg$Altern$0$1 = tmp3.p1;
  if (arg$Altern$0$1 instanceof SpecialRegExpIPv41.Empty.class) {
    inlinedVal2 = normalize_Altern_sp_2(tmp3);
  } else {
    let self5, inlinedVal8, self6, inlinedVal9;
    self5 = tmp3;
    self6 = self5;
    self6.p1;
    (new SpecialRegExpIPv41.Nil());
    self6.p2;
    (new SpecialRegExpIPv41.Nil());
    (new SpecialRegExpIPv41.Nil());
    inlinedVal9 = (new SpecialRegExpIPv41.Nothing.class());
    inlinedVal8 = inlinedVal9;
    inlinedVal2 = inlinedVal8;
  }
  p1__11 = inlinedVal2;
  self2.p1;
  tmp111 = (new SpecialRegExpIPv41.Concat.class(p1__11, self2.p2));
  tmp121 = derive_In_sp_0(self2.p2, c2);
  tmp131 = (new SpecialRegExpIPv41.Altern.class(tmp111, tmp121));
  arg$Altern$1$2 = tmp131.p2;
  if (arg$Altern$1$2 instanceof SpecialRegExpIPv41.Empty.class) {
    let self5, inlinedVal8, tmp5, self6, inlinedVal9, ls, inlinedVal10, p1__, p2__, tmp10, scrut1, arg$Concat$0$2, self7, inlinedVal11, self8, inlinedVal12, xs, ys, inlinedVal13;
    self5 = tmp131;
    self6 = self5;
    scrut1 = self6.p1;
    arg$Concat$0$2 = scrut1.p1;
    if (arg$Concat$0$2 instanceof SpecialRegExpIPv41.Empty.class) {
      let self9, inlinedVal14, self10, inlinedVal15, xs1, ys1, inlinedVal16, y, arg_Cons_0_1, tmp22, ls1, e, inlinedVal17, xs2, inlinedVal18, x2, arg_Cons_0_, tmp211, e1, inlinedVal19, ys2, inlinedVal20, y1, arg_Cons_0_11, tmp221, e2, inlinedVal21, xs3, inlinedVal22;
      self9 = self6.p1;
      inlinedVal14 = (new SpecialRegExpIPv41.Cons.class(self9, SpecialRegExpIPv41.Nil));
      p1__ = inlinedVal14;
      self10 = self6.p2;
      inlinedVal15 = (new SpecialRegExpIPv41.Cons.class(self10, SpecialRegExpIPv41.Nil));
      p2__ = inlinedVal15;
      (new SpecialRegExpIPv41.Nil());
      ys2 = p1__;
      arg_Cons_0_11 = ys2.x;
      ys2.xs;
      y1 = arg_Cons_0_11;
      e2 = y1;
      inlinedVal21 = (new SpecialRegExpIPv41.Cons.class(e2, SpecialRegExpIPv41.Nil));
      tmp221 = inlinedVal21;
      xs3 = tmp221;
      inlinedVal22 = xs3;
      inlinedVal20 = inlinedVal22;
      tmp10 = inlinedVal20;
      xs1 = tmp10;
      ys1 = p2__;
      arg_Cons_0_1 = ys1.x;
      ys1.xs;
      y = arg_Cons_0_1;
      ls1 = xs1;
      e = y;
      arg_Cons_0_ = ls1.x;
      ls1.xs;
      x2 = arg_Cons_0_;
      e1 = e;
      inlinedVal19 = (new SpecialRegExpIPv41.Cons.class(e1, SpecialRegExpIPv41.Nil));
      tmp211 = inlinedVal19;
      inlinedVal17 = (new SpecialRegExpIPv41.Cons.class(x2, tmp211));
      tmp22 = inlinedVal17;
      xs2 = tmp22;
      inlinedVal18 = xs2;
      inlinedVal16 = inlinedVal18;
      inlinedVal9 = inlinedVal16;
    } else {
      let y, arg_Cons_0_1, tmp22, ls1, e, inlinedVal14, xs1, inlinedVal15, x2, arg_Cons_0_, tmp211, e1, inlinedVal16, ys1, inlinedVal17, y1, arg_Cons_0_11, tmp221, e2, inlinedVal18, xs2, inlinedVal19;
      self7 = self6.p1;
      inlinedVal11 = (new SpecialRegExpIPv41.Cons.class(self7, SpecialRegExpIPv41.Nil));
      p1__ = inlinedVal11;
      self8 = self6.p2;
      inlinedVal12 = (new SpecialRegExpIPv41.Cons.class(self8, SpecialRegExpIPv41.Nil));
      p2__ = inlinedVal12;
      (new SpecialRegExpIPv41.Nil());
      ys1 = p1__;
      arg_Cons_0_11 = ys1.x;
      ys1.xs;
      y1 = arg_Cons_0_11;
      e2 = y1;
      inlinedVal18 = (new SpecialRegExpIPv41.Cons.class(e2, SpecialRegExpIPv41.Nil));
      tmp221 = inlinedVal18;
      xs2 = tmp221;
      inlinedVal19 = xs2;
      inlinedVal17 = inlinedVal19;
      tmp10 = inlinedVal17;
      xs = tmp10;
      ys = p2__;
      arg_Cons_0_1 = ys.x;
      ys.xs;
      y = arg_Cons_0_1;
      ls1 = xs;
      e = y;
      arg_Cons_0_ = ls1.x;
      ls1.xs;
      x2 = arg_Cons_0_;
      e1 = e;
      inlinedVal16 = (new SpecialRegExpIPv41.Cons.class(e1, SpecialRegExpIPv41.Nil));
      tmp211 = inlinedVal16;
      inlinedVal14 = (new SpecialRegExpIPv41.Cons.class(x2, tmp211));
      tmp22 = inlinedVal14;
      xs1 = tmp22;
      inlinedVal15 = xs1;
      inlinedVal13 = inlinedVal15;
      inlinedVal9 = inlinedVal13;
    }
    tmp5 = inlinedVal9;
    ls = tmp5;
    inlinedLbl: {
      let x5, xs5, arg_Cons_0_4, arg_Cons_1_4, tmp26, arg$Cons$0$, arg$Concat$0$3, arg$Concat$1$2, ls1, inlinedVal14, x4, arg_Cons_0_41;
      arg$Cons$0$ = ls.x;
      if (arg$Cons$0$ instanceof SpecialRegExpIPv41.Concat.class && (arg$Concat$0$3 = arg$Cons$0$.p1, arg$Concat$1$2 = arg$Cons$0$.p2, arg$Concat$0$3 instanceof SpecialRegExpIPv41.Empty.class) && arg$Concat$1$2 instanceof SpecialRegExpIPv41.In.class) {
        let ls2, inlinedVal15, x41, arg_Cons_0_42;
        arg_Cons_0_4 = ls.x;
        arg_Cons_1_4 = ls.xs;
        xs5 = arg_Cons_1_4;
        x5 = arg_Cons_0_4;
        ls2 = xs5;
        arg_Cons_0_42 = ls2.x;
        ls2.xs;
        x41 = arg_Cons_0_42;
        inlinedVal15 = x41;
        tmp26 = inlinedVal15;
        inlinedVal10 = (new SpecialRegExpIPv41.Altern.class(x5, tmp26));
        break inlinedLbl
      }
      arg$Concat$0$3 = arg$Cons$0$.p1;
      arg$Concat$1$2 = arg$Cons$0$.p2;
      arg_Cons_0_4 = ls.x;
      arg_Cons_1_4 = ls.xs;
      xs5 = arg_Cons_1_4;
      x5 = arg_Cons_0_4;
      ls1 = xs5;
      arg_Cons_0_41 = ls1.x;
      ls1.xs;
      x4 = arg_Cons_0_41;
      inlinedVal14 = x4;
      tmp26 = inlinedVal14;
      inlinedVal10 = (new SpecialRegExpIPv41.Altern.class(x5, tmp26));
    }
    inlinedVal8 = inlinedVal10;
    inlinedVal1 = inlinedVal8;
  } else {
    let tmp5, self5, inlinedVal8, ls, inlinedVal9, p1__, tmp10, scrut1, arg$Concat$0$2, self6, inlinedVal10, xs, inlinedVal11;
    self4 = tmp131;
    self5 = self4;
    scrut1 = self5.p1;
    arg$Concat$0$2 = scrut1.p1;
    if (arg$Concat$0$2 instanceof SpecialRegExpIPv41.Empty.class) {
      let self7, inlinedVal12, xs1, inlinedVal13, ys, inlinedVal14, y, arg_Cons_0_1, tmp22, e, inlinedVal15, xs2, inlinedVal16;
      self7 = self5.p1;
      inlinedVal12 = (new SpecialRegExpIPv41.Cons.class(self7, SpecialRegExpIPv41.Nil));
      p1__ = inlinedVal12;
      self5.p2;
      (new SpecialRegExpIPv41.Nil());
      (new SpecialRegExpIPv41.Nil());
      ys = p1__;
      arg_Cons_0_1 = ys.x;
      ys.xs;
      y = arg_Cons_0_1;
      e = y;
      inlinedVal15 = (new SpecialRegExpIPv41.Cons.class(e, SpecialRegExpIPv41.Nil));
      tmp22 = inlinedVal15;
      xs2 = tmp22;
      inlinedVal16 = xs2;
      inlinedVal14 = inlinedVal16;
      tmp10 = inlinedVal14;
      xs1 = tmp10;
      inlinedVal13 = xs1;
      inlinedVal8 = inlinedVal13;
    } else {
      let ys, inlinedVal12, y, arg_Cons_0_1, tmp22, e, inlinedVal13, xs1, inlinedVal14;
      self6 = self5.p1;
      inlinedVal10 = (new SpecialRegExpIPv41.Cons.class(self6, SpecialRegExpIPv41.Nil));
      p1__ = inlinedVal10;
      self5.p2;
      (new SpecialRegExpIPv41.Nil());
      (new SpecialRegExpIPv41.Nil());
      ys = p1__;
      arg_Cons_0_1 = ys.x;
      ys.xs;
      y = arg_Cons_0_1;
      e = y;
      inlinedVal13 = (new SpecialRegExpIPv41.Cons.class(e, SpecialRegExpIPv41.Nil));
      tmp22 = inlinedVal13;
      xs1 = tmp22;
      inlinedVal14 = xs1;
      inlinedVal12 = inlinedVal14;
      tmp10 = inlinedVal12;
      xs = tmp10;
      inlinedVal11 = xs;
      inlinedVal8 = inlinedVal11;
    }
    tmp5 = inlinedVal8;
    ls = tmp5;
    inlinedLbl: {
      let x4, arg_Cons_0_4, arg$Cons$0$, arg$Concat$0$3, arg$Concat$1$2;
      arg$Cons$0$ = ls.x;
      if (arg$Cons$0$ instanceof SpecialRegExpIPv41.Concat.class && (arg$Concat$0$3 = arg$Cons$0$.p1, arg$Concat$1$2 = arg$Cons$0$.p2, arg$Concat$0$3 instanceof SpecialRegExpIPv41.Empty.class) && arg$Concat$1$2 instanceof SpecialRegExpIPv41.In.class) {
        arg_Cons_0_4 = ls.x;
        ls.xs;
        x4 = arg_Cons_0_4;
        inlinedVal9 = x4;
        break inlinedLbl
      }
      arg$Concat$0$3 = arg$Cons$0$.p1;
      arg$Concat$1$2 = arg$Cons$0$.p2;
      arg_Cons_0_4 = ls.x;
      ls.xs;
      x4 = arg_Cons_0_4;
      inlinedVal9 = x4;
    }
    inlinedVal3 = inlinedVal9;
    inlinedVal1 = inlinedVal3;
  }
  tmp12 = inlinedVal1;
  tmp13 = (new SpecialRegExpIPv41.Altern.class(tmp11, tmp12));
  arg$Altern$1$ = tmp13.p2;
  if (arg$Altern$1$ instanceof SpecialRegExpIPv41.Altern.class && (arg$Altern$0$ = arg$Altern$1$.p1, arg$Altern$1$1 = arg$Altern$1$.p2, arg$Altern$0$ instanceof SpecialRegExpIPv41.Concat.class) && (arg$Concat$0$1 = arg$Altern$0$.p1, arg$Concat$1$1 = arg$Altern$0$.p2, arg$Concat$0$1 instanceof SpecialRegExpIPv41.Empty.class) && arg$Concat$1$1 instanceof SpecialRegExpIPv41.In.class && arg$Altern$1$1 instanceof SpecialRegExpIPv41.Empty.class) {
    let self5, inlinedVal8, tmp5, self6, inlinedVal9, p1__, p2__, tmp10, scrut1, arg$Concat$0$2, self7, inlinedVal10, self8, inlinedVal11;
    self5 = tmp13;
    self6 = self5;
    scrut1 = self6.p1;
    arg$Concat$0$2 = scrut1.p1;
    if (arg$Concat$0$2 instanceof SpecialRegExpIPv41.Empty.class) {
      let self9, inlinedVal12, self10, inlinedVal13, p1__2, p2__1, tmp9, tmp101, ys, inlinedVal14, y, arg_Cons_0_1, tmp22, e, inlinedVal15, xs, inlinedVal16;
      self9 = self6.p1;
      inlinedVal12 = (new SpecialRegExpIPv41.Cons.class(self9, SpecialRegExpIPv41.Nil));
      p1__ = inlinedVal12;
      self10 = self6.p2;
      p1__2 = runtime.safeCall(self10.p1.flat());
      p2__1 = runtime.safeCall(self10.p2.flat());
      tmp9 = (new SpecialRegExpIPv41.Nil());
      tmp101 = concatUnique_SpecialRegExpIPv4_sp_0(tmp9, p1__2);
      inlinedVal13 = SpecialRegExpIPv41.concatUnique(tmp101, p2__1);
      p2__ = inlinedVal13;
      (new SpecialRegExpIPv41.Nil());
      ys = p1__;
      arg_Cons_0_1 = ys.x;
      ys.xs;
      y = arg_Cons_0_1;
      e = y;
      inlinedVal15 = (new SpecialRegExpIPv41.Cons.class(e, SpecialRegExpIPv41.Nil));
      tmp22 = inlinedVal15;
      xs = tmp22;
      inlinedVal16 = xs;
      inlinedVal14 = inlinedVal16;
      tmp10 = inlinedVal14;
      inlinedVal9 = concatUnique_SpecialRegExpIPv4_sp_17(tmp10, p2__);
    } else {
      let p1__2, p2__1, tmp9, tmp101, ys, inlinedVal12, y, arg_Cons_0_1, tmp22, e, inlinedVal13, xs, inlinedVal14;
      self7 = self6.p1;
      inlinedVal10 = (new SpecialRegExpIPv41.Cons.class(self7, SpecialRegExpIPv41.Nil));
      p1__ = inlinedVal10;
      self8 = self6.p2;
      p1__2 = runtime.safeCall(self8.p1.flat());
      p2__1 = runtime.safeCall(self8.p2.flat());
      tmp9 = (new SpecialRegExpIPv41.Nil());
      tmp101 = concatUnique_SpecialRegExpIPv4_sp_0(tmp9, p1__2);
      inlinedVal11 = SpecialRegExpIPv41.concatUnique(tmp101, p2__1);
      p2__ = inlinedVal11;
      (new SpecialRegExpIPv41.Nil());
      ys = p1__;
      arg_Cons_0_1 = ys.x;
      ys.xs;
      y = arg_Cons_0_1;
      e = y;
      inlinedVal13 = (new SpecialRegExpIPv41.Cons.class(e, SpecialRegExpIPv41.Nil));
      tmp22 = inlinedVal13;
      xs = tmp22;
      inlinedVal14 = xs;
      inlinedVal12 = inlinedVal14;
      tmp10 = inlinedVal12;
      inlinedVal9 = concatUnique_SpecialRegExpIPv4_sp_20(tmp10, p2__);
    }
    tmp5 = inlinedVal9;
    inlinedVal8 = SpecialRegExpIPv41.mkUnion(tmp5);
    return inlinedVal8
  }
  if (arg$Altern$1$ instanceof SpecialRegExpIPv41.Altern.class) {
    let self5, inlinedVal8, tmp5, self6, inlinedVal9, p1__, p2__, tmp10, scrut1, arg$Concat$0$2, self7, inlinedVal10, self8, inlinedVal11;
    arg$Altern$0$ = arg$Altern$1$.p1;
    arg$Altern$1$1 = arg$Altern$1$.p2;
    arg$Concat$0$1 = arg$Altern$0$.p1;
    arg$Concat$1$1 = arg$Altern$0$.p2;
    self5 = tmp13;
    self6 = self5;
    scrut1 = self6.p1;
    arg$Concat$0$2 = scrut1.p1;
    if (arg$Concat$0$2 instanceof SpecialRegExpIPv41.Empty.class) {
      let self9, inlinedVal12, self10, inlinedVal13, p1__2, p2__1, tmp9, tmp101, ys, inlinedVal14, y, arg_Cons_0_1, tmp22, e, inlinedVal15, xs, inlinedVal16;
      self9 = self6.p1;
      inlinedVal12 = (new SpecialRegExpIPv41.Cons.class(self9, SpecialRegExpIPv41.Nil));
      p1__ = inlinedVal12;
      self10 = self6.p2;
      p1__2 = runtime.safeCall(self10.p1.flat());
      p2__1 = runtime.safeCall(self10.p2.flat());
      tmp9 = (new SpecialRegExpIPv41.Nil());
      tmp101 = concatUnique_SpecialRegExpIPv4_sp_0(tmp9, p1__2);
      inlinedVal13 = SpecialRegExpIPv41.concatUnique(tmp101, p2__1);
      p2__ = inlinedVal13;
      (new SpecialRegExpIPv41.Nil());
      ys = p1__;
      arg_Cons_0_1 = ys.x;
      ys.xs;
      y = arg_Cons_0_1;
      e = y;
      inlinedVal15 = (new SpecialRegExpIPv41.Cons.class(e, SpecialRegExpIPv41.Nil));
      tmp22 = inlinedVal15;
      xs = tmp22;
      inlinedVal16 = xs;
      inlinedVal14 = inlinedVal16;
      tmp10 = inlinedVal14;
      inlinedVal9 = concatUnique_SpecialRegExpIPv4_sp_17(tmp10, p2__);
    } else {
      let p1__2, p2__1, tmp9, tmp101, ys, inlinedVal12, y, arg_Cons_0_1, tmp22, e, inlinedVal13, xs, inlinedVal14;
      self7 = self6.p1;
      inlinedVal10 = (new SpecialRegExpIPv41.Cons.class(self7, SpecialRegExpIPv41.Nil));
      p1__ = inlinedVal10;
      self8 = self6.p2;
      p1__2 = runtime.safeCall(self8.p1.flat());
      p2__1 = runtime.safeCall(self8.p2.flat());
      tmp9 = (new SpecialRegExpIPv41.Nil());
      tmp101 = concatUnique_SpecialRegExpIPv4_sp_0(tmp9, p1__2);
      inlinedVal11 = SpecialRegExpIPv41.concatUnique(tmp101, p2__1);
      p2__ = inlinedVal11;
      (new SpecialRegExpIPv41.Nil());
      ys = p1__;
      arg_Cons_0_1 = ys.x;
      ys.xs;
      y = arg_Cons_0_1;
      e = y;
      inlinedVal13 = (new SpecialRegExpIPv41.Cons.class(e, SpecialRegExpIPv41.Nil));
      tmp22 = inlinedVal13;
      xs = tmp22;
      inlinedVal14 = xs;
      inlinedVal12 = inlinedVal14;
      tmp10 = inlinedVal12;
      inlinedVal9 = concatUnique_SpecialRegExpIPv4_sp_20(tmp10, p2__);
    }
    tmp5 = inlinedVal9;
    inlinedVal8 = SpecialRegExpIPv41.mkUnion(tmp5);
    return inlinedVal8
  } else if (arg$Altern$1$ instanceof SpecialRegExpIPv41.Concat.class) {
    let self5, inlinedVal8, tmp5, self6, inlinedVal9, ls, inlinedVal10, p1__, p2__, tmp10, scrut1, arg$Concat$0$2, self7, inlinedVal11, self8, inlinedVal12, xs, ys, inlinedVal13;
    arg$Concat$0$ = arg$Altern$1$.p1;
    arg$Concat$1$ = arg$Altern$1$.p2;
    if (arg$Concat$0$ instanceof SpecialRegExpIPv41.Empty.class && arg$Concat$1$ instanceof SpecialRegExpIPv41.In.class) {
      let self9, inlinedVal14, tmp51, self10, inlinedVal15, ls1, inlinedVal16, p1__2, p2__1, tmp101, scrut2, arg$Concat$0$3, self11, inlinedVal17, self12, inlinedVal18, xs1, ys1, inlinedVal19;
      self9 = tmp13;
      self10 = self9;
      scrut2 = self10.p1;
      arg$Concat$0$3 = scrut2.p1;
      if (arg$Concat$0$3 instanceof SpecialRegExpIPv41.Empty.class) {
        let self13, inlinedVal20, self14, inlinedVal21, xs2, ys2, inlinedVal22, y, arg_Cons_0_1, tmp22, ls2, e, inlinedVal23, xs3, inlinedVal24, x2, arg_Cons_0_, tmp211, e1, inlinedVal25, ys3, inlinedVal26, y1, arg_Cons_0_11, tmp221, e2, inlinedVal27, xs4, inlinedVal28;
        self13 = self10.p1;
        inlinedVal20 = (new SpecialRegExpIPv41.Cons.class(self13, SpecialRegExpIPv41.Nil));
        p1__2 = inlinedVal20;
        self14 = self10.p2;
        inlinedVal21 = (new SpecialRegExpIPv41.Cons.class(self14, SpecialRegExpIPv41.Nil));
        p2__1 = inlinedVal21;
        (new SpecialRegExpIPv41.Nil());
        ys3 = p1__2;
        arg_Cons_0_11 = ys3.x;
        ys3.xs;
        y1 = arg_Cons_0_11;
        e2 = y1;
        inlinedVal27 = (new SpecialRegExpIPv41.Cons.class(e2, SpecialRegExpIPv41.Nil));
        tmp221 = inlinedVal27;
        xs4 = tmp221;
        inlinedVal28 = xs4;
        inlinedVal26 = inlinedVal28;
        tmp101 = inlinedVal26;
        xs2 = tmp101;
        ys2 = p2__1;
        arg_Cons_0_1 = ys2.x;
        ys2.xs;
        y = arg_Cons_0_1;
        ls2 = xs2;
        e = y;
        arg_Cons_0_ = ls2.x;
        ls2.xs;
        x2 = arg_Cons_0_;
        e1 = e;
        inlinedVal25 = (new SpecialRegExpIPv41.Cons.class(e1, SpecialRegExpIPv41.Nil));
        tmp211 = inlinedVal25;
        inlinedVal23 = (new SpecialRegExpIPv41.Cons.class(x2, tmp211));
        tmp22 = inlinedVal23;
        xs3 = tmp22;
        inlinedVal24 = xs3;
        inlinedVal22 = inlinedVal24;
        inlinedVal15 = inlinedVal22;
      } else {
        let y, arg_Cons_0_1, tmp22, ls2, e, inlinedVal20, xs2, inlinedVal21, x2, arg_Cons_0_, tmp211, e1, inlinedVal22, ys2, inlinedVal23, y1, arg_Cons_0_11, tmp221, e2, inlinedVal24, xs3, inlinedVal25;
        self11 = self10.p1;
        inlinedVal17 = (new SpecialRegExpIPv41.Cons.class(self11, SpecialRegExpIPv41.Nil));
        p1__2 = inlinedVal17;
        self12 = self10.p2;
        inlinedVal18 = (new SpecialRegExpIPv41.Cons.class(self12, SpecialRegExpIPv41.Nil));
        p2__1 = inlinedVal18;
        (new SpecialRegExpIPv41.Nil());
        ys2 = p1__2;
        arg_Cons_0_11 = ys2.x;
        ys2.xs;
        y1 = arg_Cons_0_11;
        e2 = y1;
        inlinedVal24 = (new SpecialRegExpIPv41.Cons.class(e2, SpecialRegExpIPv41.Nil));
        tmp221 = inlinedVal24;
        xs3 = tmp221;
        inlinedVal25 = xs3;
        inlinedVal23 = inlinedVal25;
        tmp101 = inlinedVal23;
        xs1 = tmp101;
        ys1 = p2__1;
        arg_Cons_0_1 = ys1.x;
        ys1.xs;
        y = arg_Cons_0_1;
        ls2 = xs1;
        e = y;
        arg_Cons_0_ = ls2.x;
        ls2.xs;
        x2 = arg_Cons_0_;
        e1 = e;
        inlinedVal22 = (new SpecialRegExpIPv41.Cons.class(e1, SpecialRegExpIPv41.Nil));
        tmp211 = inlinedVal22;
        inlinedVal20 = (new SpecialRegExpIPv41.Cons.class(x2, tmp211));
        tmp22 = inlinedVal20;
        xs2 = tmp22;
        inlinedVal21 = xs2;
        inlinedVal19 = inlinedVal21;
        inlinedVal15 = inlinedVal19;
      }
      tmp51 = inlinedVal15;
      ls1 = tmp51;
      inlinedLbl: {
        let x5, xs5, arg_Cons_0_4, arg_Cons_1_4, tmp26, arg$Cons$0$, arg$Concat$0$4, arg$Concat$1$2, ls2, inlinedVal20, x4, arg_Cons_0_41;
        arg$Cons$0$ = ls1.x;
        if (arg$Cons$0$ instanceof SpecialRegExpIPv41.Concat.class && (arg$Concat$0$4 = arg$Cons$0$.p1, arg$Concat$1$2 = arg$Cons$0$.p2, arg$Concat$0$4 instanceof SpecialRegExpIPv41.Empty.class) && arg$Concat$1$2 instanceof SpecialRegExpIPv41.Concat.class) {
          let ls3, inlinedVal21, x41, arg_Cons_0_42;
          arg_Cons_0_4 = ls1.x;
          arg_Cons_1_4 = ls1.xs;
          xs5 = arg_Cons_1_4;
          x5 = arg_Cons_0_4;
          ls3 = xs5;
          arg_Cons_0_42 = ls3.x;
          ls3.xs;
          x41 = arg_Cons_0_42;
          inlinedVal21 = x41;
          tmp26 = inlinedVal21;
          inlinedVal16 = (new SpecialRegExpIPv41.Altern.class(x5, tmp26));
          break inlinedLbl
        }
        arg$Concat$0$4 = arg$Cons$0$.p1;
        arg$Concat$1$2 = arg$Cons$0$.p2;
        arg_Cons_0_4 = ls1.x;
        arg_Cons_1_4 = ls1.xs;
        xs5 = arg_Cons_1_4;
        x5 = arg_Cons_0_4;
        ls2 = xs5;
        arg_Cons_0_41 = ls2.x;
        ls2.xs;
        x4 = arg_Cons_0_41;
        inlinedVal20 = x4;
        tmp26 = inlinedVal20;
        inlinedVal16 = (new SpecialRegExpIPv41.Altern.class(x5, tmp26));
      }
      inlinedVal14 = inlinedVal16;
      return inlinedVal14
    }
    self5 = tmp13;
    self6 = self5;
    scrut1 = self6.p1;
    arg$Concat$0$2 = scrut1.p1;
    if (arg$Concat$0$2 instanceof SpecialRegExpIPv41.Empty.class) {
      let self9, inlinedVal14, self10, inlinedVal15, xs1, ys1, inlinedVal16, y, arg_Cons_0_1, tmp22, ls1, e, inlinedVal17, xs2, inlinedVal18, x2, arg_Cons_0_, tmp211, e1, inlinedVal19, ys2, inlinedVal20, y1, arg_Cons_0_11, tmp221, e2, inlinedVal21, xs3, inlinedVal22;
      self9 = self6.p1;
      inlinedVal14 = (new SpecialRegExpIPv41.Cons.class(self9, SpecialRegExpIPv41.Nil));
      p1__ = inlinedVal14;
      self10 = self6.p2;
      inlinedVal15 = (new SpecialRegExpIPv41.Cons.class(self10, SpecialRegExpIPv41.Nil));
      p2__ = inlinedVal15;
      (new SpecialRegExpIPv41.Nil());
      ys2 = p1__;
      arg_Cons_0_11 = ys2.x;
      ys2.xs;
      y1 = arg_Cons_0_11;
      e2 = y1;
      inlinedVal21 = (new SpecialRegExpIPv41.Cons.class(e2, SpecialRegExpIPv41.Nil));
      tmp221 = inlinedVal21;
      xs3 = tmp221;
      inlinedVal22 = xs3;
      inlinedVal20 = inlinedVal22;
      tmp10 = inlinedVal20;
      xs1 = tmp10;
      ys1 = p2__;
      arg_Cons_0_1 = ys1.x;
      ys1.xs;
      y = arg_Cons_0_1;
      ls1 = xs1;
      e = y;
      arg_Cons_0_ = ls1.x;
      ls1.xs;
      x2 = arg_Cons_0_;
      e1 = e;
      inlinedVal19 = (new SpecialRegExpIPv41.Cons.class(e1, SpecialRegExpIPv41.Nil));
      tmp211 = inlinedVal19;
      inlinedVal17 = (new SpecialRegExpIPv41.Cons.class(x2, tmp211));
      tmp22 = inlinedVal17;
      xs2 = tmp22;
      inlinedVal18 = xs2;
      inlinedVal16 = inlinedVal18;
      inlinedVal9 = inlinedVal16;
    } else {
      let y, arg_Cons_0_1, tmp22, ls1, e, inlinedVal14, xs1, inlinedVal15, x2, arg_Cons_0_, tmp211, e1, inlinedVal16, ys1, inlinedVal17, y1, arg_Cons_0_11, tmp221, e2, inlinedVal18, xs2, inlinedVal19;
      self7 = self6.p1;
      inlinedVal11 = (new SpecialRegExpIPv41.Cons.class(self7, SpecialRegExpIPv41.Nil));
      p1__ = inlinedVal11;
      self8 = self6.p2;
      inlinedVal12 = (new SpecialRegExpIPv41.Cons.class(self8, SpecialRegExpIPv41.Nil));
      p2__ = inlinedVal12;
      (new SpecialRegExpIPv41.Nil());
      ys1 = p1__;
      arg_Cons_0_11 = ys1.x;
      ys1.xs;
      y1 = arg_Cons_0_11;
      e2 = y1;
      inlinedVal18 = (new SpecialRegExpIPv41.Cons.class(e2, SpecialRegExpIPv41.Nil));
      tmp221 = inlinedVal18;
      xs2 = tmp221;
      inlinedVal19 = xs2;
      inlinedVal17 = inlinedVal19;
      tmp10 = inlinedVal17;
      xs = tmp10;
      ys = p2__;
      arg_Cons_0_1 = ys.x;
      ys.xs;
      y = arg_Cons_0_1;
      ls1 = xs;
      e = y;
      arg_Cons_0_ = ls1.x;
      ls1.xs;
      x2 = arg_Cons_0_;
      e1 = e;
      inlinedVal16 = (new SpecialRegExpIPv41.Cons.class(e1, SpecialRegExpIPv41.Nil));
      tmp211 = inlinedVal16;
      inlinedVal14 = (new SpecialRegExpIPv41.Cons.class(x2, tmp211));
      tmp22 = inlinedVal14;
      xs1 = tmp22;
      inlinedVal15 = xs1;
      inlinedVal13 = inlinedVal15;
      inlinedVal9 = inlinedVal13;
    }
    tmp5 = inlinedVal9;
    ls = tmp5;
    inlinedLbl: {
      let x5, xs5, arg_Cons_0_4, arg_Cons_1_4, tmp26, arg$Cons$0$, arg$Concat$0$3, arg$Concat$1$2, ls1, inlinedVal14, x4, arg_Cons_0_41;
      arg$Cons$0$ = ls.x;
      if (arg$Cons$0$ instanceof SpecialRegExpIPv41.Concat.class && (arg$Concat$0$3 = arg$Cons$0$.p1, arg$Concat$1$2 = arg$Cons$0$.p2, arg$Concat$0$3 instanceof SpecialRegExpIPv41.Empty.class) && arg$Concat$1$2 instanceof SpecialRegExpIPv41.Concat.class) {
        let ls2, inlinedVal15, x41, arg_Cons_0_42;
        arg_Cons_0_4 = ls.x;
        arg_Cons_1_4 = ls.xs;
        xs5 = arg_Cons_1_4;
        x5 = arg_Cons_0_4;
        ls2 = xs5;
        arg_Cons_0_42 = ls2.x;
        ls2.xs;
        x41 = arg_Cons_0_42;
        inlinedVal15 = x41;
        tmp26 = inlinedVal15;
        inlinedVal10 = (new SpecialRegExpIPv41.Altern.class(x5, tmp26));
        break inlinedLbl
      }
      arg$Concat$0$3 = arg$Cons$0$.p1;
      arg$Concat$1$2 = arg$Cons$0$.p2;
      arg_Cons_0_4 = ls.x;
      arg_Cons_1_4 = ls.xs;
      xs5 = arg_Cons_1_4;
      x5 = arg_Cons_0_4;
      ls1 = xs5;
      arg_Cons_0_41 = ls1.x;
      ls1.xs;
      x4 = arg_Cons_0_41;
      inlinedVal14 = x4;
      tmp26 = inlinedVal14;
      inlinedVal10 = (new SpecialRegExpIPv41.Altern.class(x5, tmp26));
    }
    inlinedVal8 = inlinedVal10;
    return inlinedVal8
  }
};
eq_Concat_sp_4 = function eq_Concat_sp_4(self, other) {
  let p2__1, p1__3, scrut6, scrut7, arg_Concat_0_, arg_Concat_1_;
  if (other instanceof SpecialRegExpIPv41.Concat.class) {
    arg_Concat_0_ = other.p1;
    arg_Concat_1_ = other.p2;
    p2__1 = arg_Concat_1_;
    p1__3 = arg_Concat_0_;
    scrut6 = runtime.safeCall(self.p1.eq(p1__3));
    if (scrut6 === true) {
      scrut7 = runtime.safeCall(self.p2.eq(p2__1));
      if (scrut7 === true) {
        return true
      }
      return false;
    }
    return false;
  }
  return false;
};
normalize_Concat_sp_17 = function normalize_Concat_sp_17(self) {
  let p1__2, tmp15, self1, inlinedVal, self2, inlinedVal1, p1__21, tmp151, self3, inlinedVal2, p1__22, tmp152, self4, inlinedVal3;
  self1 = self.p1;
  p1__22 = normalize_Altern_sp_14(self1.p1);
  self4 = self1.p2;
  inlinedVal3 = self4;
  tmp152 = inlinedVal3;
  inlinedVal = (new SpecialRegExpIPv41.Concat.class(p1__22, tmp152));
  p1__2 = inlinedVal;
  self2 = self.p2;
  p1__21 = normalize_Altern_sp_14(self2.p1);
  self3 = self2.p2;
  inlinedVal2 = self3;
  tmp151 = inlinedVal2;
  inlinedVal1 = (new SpecialRegExpIPv41.Concat.class(p1__21, tmp151));
  tmp15 = inlinedVal1;
  return (new SpecialRegExpIPv41.Concat.class(p1__2, tmp15))
};
normalize_Concat_sp_25 = function normalize_Concat_sp_25(self) {
  let p1__2, tmp15, self1, inlinedVal, self2, inlinedVal1, p1__21, tmp151, self3, inlinedVal2;
  self1 = self.p1;
  inlinedVal = self1;
  p1__2 = inlinedVal;
  self2 = self.p2;
  p1__21 = normalize_Altern_sp_14(self2.p1);
  self3 = self2.p2;
  inlinedVal2 = self3;
  tmp151 = inlinedVal2;
  inlinedVal1 = (new SpecialRegExpIPv41.Concat.class(p1__21, tmp151));
  tmp15 = inlinedVal1;
  return (new SpecialRegExpIPv41.Concat.class(p1__2, tmp15))
};
normalize_Concat_sp_8 = function normalize_Concat_sp_8(self) {
  let p1__2, tmp15;
  p1__2 = runtime.safeCall(self.p1.normalize());
  if (p1__2 instanceof SpecialRegExpIPv41.Empty.class) {
    let self1, inlinedVal;
    self1 = self.p2;
    inlinedVal = self1;
    return inlinedVal
  } else if (p1__2 instanceof SpecialRegExpIPv41.Nothing.class) {
    return p1__2
  }
  {
    let self1, inlinedVal;
    self1 = self.p2;
    inlinedVal = self1;
    tmp15 = inlinedVal;
    return (new SpecialRegExpIPv41.Concat.class(p1__2, tmp15));
  }
};
startsWith_Concat_sp_14 = function startsWith_Concat_sp_14(self, c) {
  let scrut8, scrut9, tmp16;
  tmp16 = runtime.safeCall(self.p1.startsWith(c));
  if (tmp16 === false) {
    scrut8 = runtime.safeCall(self.p1.canBeEmpty());
    if (scrut8 === true) {
      let c1, inlinedVal;
      self.p2;
      c1 = c;
      inlinedVal = "." == c1;
      scrut9 = inlinedVal;
      if (scrut9 === true) {
        return true
      }
      return false;
    }
    return false;
  }
  return true;
};
startsWith_Concat_sp_16 = function startsWith_Concat_sp_16(self, c) {
  let scrut8, scrut9, tmp16;
  tmp16 = runtime.safeCall(self.p1.startsWith(c));
  if (tmp16 === false) {
    scrut8 = runtime.safeCall(self.p1.canBeEmpty());
    if (scrut8 === true) {
      scrut9 = startsWith_Altern_sp_2(self.p2, c);
      if (scrut9 === true) {
        return true
      }
      return false;
    }
    return false;
  }
  return true;
};
(class SpecialRegExpIPv4 {
  static {
    SpecialRegExpIPv41 = this
  }
  static {
    SpecialRegExpIPv4.Some = function Some(x) {
      return (new Some.class(x));
    };
    (class Some {
      static {
        SpecialRegExpIPv4.Some.class = this
      }
      constructor(x) {
        this.x = x;
      }
      toString() { return runtime.render(this); }
      static [definitionMetadata] = ["class", "Some", ["x"]];
    });
    (class None {
      static {
        SpecialRegExpIPv4.None = this
      }
      toString() { return runtime.render(this); }
      static [definitionMetadata] = ["class", "None"];
    });
    SpecialRegExpIPv4.Cons = function Cons(x, xs) {
      return (new Cons.class(x, xs));
    };
    (class Cons {
      static {
        SpecialRegExpIPv4.Cons.class = this
      }
      constructor(x, xs) {
        this.x = x;
        this.xs = xs;
      }
      toString() { return runtime.render(this); }
      static [definitionMetadata] = ["class", "Cons", ["x", "xs"]];
    });
    (class Nil {
      static {
        SpecialRegExpIPv4.Nil = this
      }
      toString() { return runtime.render(this); }
      static [definitionMetadata] = ["class", "Nil"];
    });
    SpecialRegExpIPv4.RegExp = function RegExp() {
      return (new RegExp.class());
    };
    (class RegExp {
      static {
        SpecialRegExpIPv4.RegExp.class = this
      }
      toString() { return runtime.render(this); }
      static [definitionMetadata] = ["class", "RegExp", []];
    });
    SpecialRegExpIPv4.Nothing = function Nothing() {
      return (new Nothing.class());
    };
    (class Nothing extends SpecialRegExpIPv4.RegExp.class {
      static {
        SpecialRegExpIPv4.Nothing.class = this
      }
      constructor() {
        super();
      }
      canBeEmpty() {
        return false
      }
      derive(c) {
        return this
      }
      eq(other) {
        if (other instanceof SpecialRegExpIPv4.Nothing.class) {
          return true
        }
        return false;
      }
      flat() {
        return (new SpecialRegExpIPv4.Nil())
      }
      normalize() {
        return this
      }
      startsWith(c) {
        return false
      }
      toString() { return runtime.render(this); }
      static [definitionMetadata] = ["class", "Nothing", []];
    });
    SpecialRegExpIPv4.Empty = function Empty() {
      return (new Empty.class());
    };
    (class Empty extends SpecialRegExpIPv4.RegExp.class {
      static {
        SpecialRegExpIPv4.Empty.class = this
      }
      constructor() {
        super();
      }
      canBeEmpty() {
        return true
      }
      derive(c) {
        return (new SpecialRegExpIPv4.Nothing.class())
      }
      eq(other) {
        if (other instanceof SpecialRegExpIPv4.Empty.class) {
          return true
        }
        return false;
      }
      flat() {
        return (new SpecialRegExpIPv4.Cons.class(this, SpecialRegExpIPv4.Nil))
      }
      normalize() {
        return this
      }
      startsWith(c) {
        return false
      }
      toString() { return runtime.render(this); }
      static [definitionMetadata] = ["class", "Empty", []];
    });
    SpecialRegExpIPv4.Exact = function Exact(ch) {
      return (new Exact.class(ch));
    };
    (class Exact extends SpecialRegExpIPv4.RegExp.class {
      static {
        SpecialRegExpIPv4.Exact.class = this
      }
      constructor(ch) {
        super();
        this.ch = ch;
      }
      canBeEmpty() {
        return false
      }
      derive(c) {
        let scrut;
        scrut = this.startsWith(c);
        if (scrut === true) {
          return (new SpecialRegExpIPv4.Empty.class())
        }
        return (new SpecialRegExpIPv4.Nothing.class());
      }
      eq(other) {
        let ch__, arg_Exact_0_;
        if (other instanceof SpecialRegExpIPv4.Exact.class) {
          arg_Exact_0_ = other.ch;
          ch__ = arg_Exact_0_;
          return this.ch == ch__
        }
        return false;
      }
      flat() {
        return (new SpecialRegExpIPv4.Cons.class(this, SpecialRegExpIPv4.Nil))
      }
      normalize() {
        return this
      }
      startsWith(c) {
        return this.ch == c
      }
      toString() { return runtime.render(this); }
      static [definitionMetadata] = ["class", "Exact", ["ch"]];
    });
    SpecialRegExpIPv4.Any = function Any() {
      return (new Any.class());
    };
    (class Any extends SpecialRegExpIPv4.RegExp.class {
      static {
        SpecialRegExpIPv4.Any.class = this
      }
      constructor() {
        super();
      }
      canBeEmpty() {
        return false
      }
      derive(c) {
        return (new SpecialRegExpIPv4.Empty.class())
      }
      eq(other) {
        if (other instanceof SpecialRegExpIPv4.Any.class) {
          return true
        }
        return false;
      }
      flat() {
        return (new SpecialRegExpIPv4.Cons.class(this, SpecialRegExpIPv4.Nil))
      }
      normalize() {
        return this
      }
      startsWith(c) {
        return true
      }
      toString() { return runtime.render(this); }
      static [definitionMetadata] = ["class", "Any", []];
    });
    SpecialRegExpIPv4.Not = function Not(chars) {
      return (new Not.class(chars));
    };
    (class Not extends SpecialRegExpIPv4.RegExp.class {
      static {
        SpecialRegExpIPv4.Not.class = this
      }
      constructor(chars) {
        super();
        this.chars = chars;
      }
      canBeEmpty() {
        return false
      }
      derive(c) {
        let scrut1;
        scrut1 = this.startsWith(c);
        if (scrut1 === true) {
          return (new SpecialRegExpIPv4.Empty.class())
        }
        return (new SpecialRegExpIPv4.Nothing.class());
      }
      eq(other) {
        let chars__, arg_Not_0_;
        if (other instanceof SpecialRegExpIPv4.Not.class) {
          arg_Not_0_ = other.chars;
          chars__ = arg_Not_0_;
          return SpecialRegExpIPv4.arrEq(this.chars, chars__)
        }
        return false;
      }
      flat() {
        return (new SpecialRegExpIPv4.Cons.class(this, SpecialRegExpIPv4.Nil))
      }
      normalize() {
        return this
      }
      startsWith(c) {
        let tmp;
        tmp = SpecialRegExpIPv4.has(this.chars, c);
        return ! tmp
      }
      toString() { return runtime.render(this); }
      static [definitionMetadata] = ["class", "Not", ["chars"]];
    });
    SpecialRegExpIPv4.Altern = function Altern(p1, p2) {
      return (new Altern.class(p1, p2));
    };
    (class Altern extends SpecialRegExpIPv4.RegExp.class {
      static {
        SpecialRegExpIPv4.Altern.class = this
      }
      constructor(p1, p2) {
        super();
        this.p1 = p1;
        this.p2 = p2;
      }
      canBeEmpty() {
        let tmp4;
        tmp4 = runtime.safeCall(this.p1.canBeEmpty());
        if (tmp4 === false) {
          return runtime.safeCall(this.p2.canBeEmpty())
        }
        return true;
      }
      derive(c) {
        let tmp1, tmp2, tmp3, self, inlinedVal, tmp5, self1, inlinedVal1, p1__, p2__, tmp9, tmp10;
        tmp1 = runtime.safeCall(this.p1.derive(c));
        tmp2 = runtime.safeCall(this.p2.derive(c));
        tmp3 = (new SpecialRegExpIPv4.Altern.class(tmp1, tmp2));
        self = tmp3;
        self1 = self;
        p1__ = runtime.safeCall(self1.p1.flat());
        p2__ = runtime.safeCall(self1.p2.flat());
        tmp9 = (new SpecialRegExpIPv41.Nil());
        tmp10 = concatUnique_SpecialRegExpIPv4_sp_0(tmp9, p1__);
        inlinedVal1 = SpecialRegExpIPv41.concatUnique(tmp10, p2__);
        tmp5 = inlinedVal1;
        inlinedVal = SpecialRegExpIPv41.mkUnion(tmp5);
        return inlinedVal
      }
      eq(other) {
        let tmp6, tmp7;
        if (other instanceof SpecialRegExpIPv4.Altern.class) {
          let self, inlinedVal, p1__, p2__, tmp9, tmp10;
          tmp6 = this.flat();
          self = other;
          p1__ = runtime.safeCall(self.p1.flat());
          p2__ = runtime.safeCall(self.p2.flat());
          tmp9 = (new SpecialRegExpIPv41.Nil());
          tmp10 = concatUnique_SpecialRegExpIPv4_sp_0(tmp9, p1__);
          inlinedVal = SpecialRegExpIPv41.concatUnique(tmp10, p2__);
          tmp7 = inlinedVal;
          return SpecialRegExpIPv4.lsEq(tmp6, tmp7)
        }
        return false;
      }
      flat() {
        let p1__, p2__, tmp9, tmp10;
        p1__ = runtime.safeCall(this.p1.flat());
        p2__ = runtime.safeCall(this.p2.flat());
        tmp9 = (new SpecialRegExpIPv4.Nil());
        tmp10 = concatUnique_SpecialRegExpIPv4_sp_0(tmp9, p1__);
        return SpecialRegExpIPv4.concatUnique(tmp10, p2__)
      }
      normalize() {
        let tmp5;
        tmp5 = this.flat();
        return SpecialRegExpIPv4.mkUnion(tmp5)
      }
      startsWith(c) {
        let tmp8;
        tmp8 = runtime.safeCall(this.p1.startsWith(c));
        if (tmp8 === false) {
          return runtime.safeCall(this.p2.startsWith(c))
        }
        return true;
      }
      toString() { return runtime.render(this); }
      static [definitionMetadata] = ["class", "Altern", ["p1", "p2"]];
    });
    SpecialRegExpIPv4.In = function In(chars) {
      return (new In.class(chars));
    };
    (class In extends SpecialRegExpIPv4.RegExp.class {
      static {
        SpecialRegExpIPv4.In.class = this
      }
      constructor(chars) {
        super();
        this.chars = chars;
      }
      canBeEmpty() {
        return false
      }
      derive(c) {
        let scrut2;
        scrut2 = this.startsWith(c);
        if (scrut2 === true) {
          return (new SpecialRegExpIPv4.Empty.class())
        }
        return (new SpecialRegExpIPv4.Nothing.class());
      }
      eq(other) {
        let chars__1, arg_In_0_;
        if (other instanceof SpecialRegExpIPv4.In.class) {
          arg_In_0_ = other.chars;
          chars__1 = arg_In_0_;
          return SpecialRegExpIPv4.arrEq(this.chars, chars__1)
        }
        return false;
      }
      flat() {
        return (new SpecialRegExpIPv4.Cons.class(this, SpecialRegExpIPv4.Nil))
      }
      normalize() {
        return this
      }
      startsWith(c) {
        return SpecialRegExpIPv4.has(this.chars, c)
      }
      toString() { return runtime.render(this); }
      static [definitionMetadata] = ["class", "In", ["chars"]];
    });
    SpecialRegExpIPv4.Concat = function Concat(p1, p2) {
      return (new Concat.class(p1, p2));
    };
    (class Concat extends SpecialRegExpIPv4.RegExp.class {
      static {
        SpecialRegExpIPv4.Concat.class = this
      }
      constructor(p1, p2) {
        super();
        this.p1 = p1;
        this.p2 = p2;
      }
      canBeEmpty() {
        let scrut4, scrut5;
        scrut4 = runtime.safeCall(this.p1.canBeEmpty());
        if (scrut4 === true) {
          scrut5 = runtime.safeCall(this.p2.canBeEmpty());
          if (scrut5 === true) {
            return true
          }
          return false;
        }
        return false;
      }
      derive(c) {
        let p1__1, scrut3, tmp11, tmp12, tmp13, tmp14;
        p1__1 = runtime.safeCall(this.p1.derive(c));
        scrut3 = runtime.safeCall(this.p1.canBeEmpty());
        if (scrut3 === true) {
          let self, inlinedVal, tmp5, self1, inlinedVal1, p1__, p2__, tmp10, self2, inlinedVal2, ys, inlinedVal3, xs, ys1, inlinedVal4, y, arg_Cons_0_1, tmp22, e, inlinedVal5, xs1, inlinedVal6, y1, ys__, arg_Cons_0_11, arg_Cons_1_1, tmp221;
          tmp11 = (new SpecialRegExpIPv4.Concat.class(p1__1, this.p2));
          tmp12 = runtime.safeCall(this.p2.derive(c));
          tmp13 = (new SpecialRegExpIPv4.Altern.class(tmp11, tmp12));
          self = tmp13;
          self1 = self;
          self2 = self1.p1;
          inlinedVal2 = (new SpecialRegExpIPv41.Cons.class(self2, SpecialRegExpIPv41.Nil));
          p1__ = inlinedVal2;
          p2__ = runtime.safeCall(self1.p2.flat());
          (new SpecialRegExpIPv41.Nil());
          ys = p1__;
          arg_Cons_0_1 = ys.x;
          ys.xs;
          y = arg_Cons_0_1;
          e = y;
          inlinedVal5 = (new SpecialRegExpIPv41.Cons.class(e, SpecialRegExpIPv41.Nil));
          tmp22 = inlinedVal5;
          xs1 = tmp22;
          inlinedVal6 = xs1;
          inlinedVal3 = inlinedVal6;
          tmp10 = inlinedVal3;
          xs = tmp10;
          ys1 = p2__;
          if (ys1 instanceof SpecialRegExpIPv41.Cons.class) {
            let ls, e1, inlinedVal7, x2, scrut10, arg_Cons_0_, tmp21, self3, other, inlinedVal8, p2__1, p1__3, scrut6, scrut7, arg_Concat_0_, arg_Concat_1_;
            arg_Cons_0_11 = ys1.x;
            arg_Cons_1_1 = ys1.xs;
            ys__ = arg_Cons_1_1;
            y1 = arg_Cons_0_11;
            ls = xs;
            e1 = y1;
            arg_Cons_0_ = ls.x;
            ls.xs;
            x2 = arg_Cons_0_;
            self3 = x2;
            other = e1;
            if (other instanceof SpecialRegExpIPv41.Concat.class) {
              arg_Concat_0_ = other.p1;
              arg_Concat_1_ = other.p2;
              p2__1 = arg_Concat_1_;
              p1__3 = arg_Concat_0_;
              scrut6 = runtime.safeCall(self3.p1.eq(p1__3));
              if (scrut6 === true) {
                scrut7 = runtime.safeCall(self3.p2.eq(p2__1));
                if (scrut7 === true) {
                  inlinedVal8 = true;
                } else {
                  inlinedVal8 = false;
                }
              } else {
                inlinedVal8 = false;
              }
            } else {
              inlinedVal8 = false;
            }
            scrut10 = inlinedVal8;
            if (scrut10 === true) {
              inlinedVal7 = ls;
            } else {
              let e2, inlinedVal9;
              e2 = e1;
              inlinedVal9 = (new SpecialRegExpIPv41.Cons.class(e2, SpecialRegExpIPv41.Nil));
              tmp21 = inlinedVal9;
              inlinedVal7 = (new SpecialRegExpIPv41.Cons.class(x2, tmp21));
            }
            tmp221 = inlinedVal7;
            inlinedVal4 = SpecialRegExpIPv41.concatUnique(tmp221, ys__);
          } else {
            inlinedVal4 = xs;
          }
          inlinedVal1 = inlinedVal4;
          tmp5 = inlinedVal1;
          inlinedVal = SpecialRegExpIPv41.mkUnion(tmp5);
          return inlinedVal
        }
        {
          let self, inlinedVal, p1__2, tmp15;
          tmp14 = (new SpecialRegExpIPv4.Concat.class(p1__1, this.p2));
          self = tmp14;
          p1__2 = runtime.safeCall(self.p1.normalize());
          if (p1__2 instanceof SpecialRegExpIPv41.Empty.class) {
            inlinedVal = runtime.safeCall(self.p2.normalize());
            return inlinedVal
          } else if (p1__2 instanceof SpecialRegExpIPv41.Nothing.class) {
            inlinedVal = p1__2;
            return inlinedVal
          }
          tmp15 = runtime.safeCall(self.p2.normalize());
          inlinedVal = (new SpecialRegExpIPv41.Concat.class(p1__2, tmp15));
          return inlinedVal;
        }
      }
      eq(other) {
        let p2__1, p1__3, scrut6, scrut7, arg_Concat_0_, arg_Concat_1_;
        if (other instanceof SpecialRegExpIPv4.Concat.class) {
          arg_Concat_0_ = other.p1;
          arg_Concat_1_ = other.p2;
          p2__1 = arg_Concat_1_;
          p1__3 = arg_Concat_0_;
          scrut6 = runtime.safeCall(this.p1.eq(p1__3));
          if (scrut6 === true) {
            scrut7 = runtime.safeCall(this.p2.eq(p2__1));
            if (scrut7 === true) {
              return true
            }
            return false;
          }
          return false;
        }
        return false;
      }
      flat() {
        return (new SpecialRegExpIPv4.Cons.class(this, SpecialRegExpIPv4.Nil))
      }
      normalize() {
        let p1__2, tmp15;
        p1__2 = runtime.safeCall(this.p1.normalize());
        if (p1__2 instanceof SpecialRegExpIPv4.Empty.class) {
          return runtime.safeCall(this.p2.normalize())
        } else if (p1__2 instanceof SpecialRegExpIPv4.Nothing.class) {
          return p1__2
        }
        tmp15 = runtime.safeCall(this.p2.normalize());
        return (new SpecialRegExpIPv4.Concat.class(p1__2, tmp15));
      }
      startsWith(c) {
        let scrut8, scrut9, tmp16;
        tmp16 = runtime.safeCall(this.p1.startsWith(c));
        if (tmp16 === false) {
          scrut8 = runtime.safeCall(this.p1.canBeEmpty());
          if (scrut8 === true) {
            scrut9 = runtime.safeCall(this.p2.startsWith(c));
            if (scrut9 === true) {
              return true
            }
            return false;
          }
          return false;
        }
        return true;
      }
      toString() { return runtime.render(this); }
      static [definitionMetadata] = ["class", "Concat", ["p1", "p2"]];
    });
    SpecialRegExpIPv4.Star = function Star(p) {
      return (new Star.class(p));
    };
    (class Star extends SpecialRegExpIPv4.RegExp.class {
      static {
        SpecialRegExpIPv4.Star.class = this
      }
      constructor(p) {
        super();
        this.p = p;
      }
      canBeEmpty() {
        return true
      }
      derive(c) {
        let tmp17, tmp18, tmp19, self, inlinedVal, p1__2, tmp15;
        tmp17 = runtime.safeCall(this.p.derive(c));
        tmp18 = (new SpecialRegExpIPv4.Star.class(this.p));
        tmp19 = (new SpecialRegExpIPv4.Concat.class(tmp17, tmp18));
        self = tmp19;
        p1__2 = runtime.safeCall(self.p1.normalize());
        if (p1__2 instanceof SpecialRegExpIPv41.Empty.class) {
          let self1, inlinedVal1, tmp20;
          self1 = self.p2;
          tmp20 = runtime.safeCall(self1.p.normalize());
          inlinedVal1 = (new SpecialRegExpIPv41.Star.class(tmp20));
          inlinedVal = inlinedVal1;
          return inlinedVal
        } else if (p1__2 instanceof SpecialRegExpIPv41.Nothing.class) {
          inlinedVal = p1__2;
          return inlinedVal
        }
        {
          let self1, inlinedVal1, tmp20;
          self1 = self.p2;
          tmp20 = runtime.safeCall(self1.p.normalize());
          inlinedVal1 = (new SpecialRegExpIPv41.Star.class(tmp20));
          tmp15 = inlinedVal1;
          inlinedVal = (new SpecialRegExpIPv41.Concat.class(p1__2, tmp15));
          return inlinedVal;
        }
      }
      eq(other) {
        let p__, arg_Star_0_;
        if (other instanceof SpecialRegExpIPv4.Star.class) {
          arg_Star_0_ = other.p;
          p__ = arg_Star_0_;
          return runtime.safeCall(this.p.eq(p__))
        }
        return false;
      }
      flat() {
        return (new SpecialRegExpIPv4.Cons.class(this, SpecialRegExpIPv4.Nil))
      }
      normalize() {
        let tmp20;
        tmp20 = runtime.safeCall(this.p.normalize());
        return (new SpecialRegExpIPv4.Star.class(tmp20))
      }
      startsWith(c) {
        return runtime.safeCall(this.p.startsWith(c))
      }
      toString() { return runtime.render(this); }
      static [definitionMetadata] = ["class", "Star", ["p"]];
    });
  }
  static arrEq(arr1, arr2) {
    return runtime.safeCall(SpecialRegExpIPv4__Legacy["SeqHelper$SpecialRegExpIPv4"].eq(arr1, arr2))
  }
  static concatUnique(xs, ys) {
    loopLabel: while (true) {
      let y, ys__, arg_Cons_0_1, arg_Cons_1_1, tmp22;
      if (ys instanceof SpecialRegExpIPv4.Cons.class) {
        arg_Cons_0_1 = ys.x;
        arg_Cons_1_1 = ys.xs;
        ys__ = arg_Cons_1_1;
        y = arg_Cons_0_1;
        tmp22 = SpecialRegExpIPv4.pushUnique(xs, y);
        xs = tmp22;
        ys = ys__;
        continue loopLabel
      }
      return xs;
    }
  }
  static digits() {
    let tmp30, tup_6;
    tup_6 = ([
      "0",
      "1",
      "2",
      "3",
      "4",
      "5",
      "6",
      "7",
      "8",
      "9"
    ]);
    tmp30 = tup_6;
    return (new SpecialRegExpIPv4.In.class(tmp30))
  }
  static has(arr, ele) {
    return runtime.safeCall(SpecialRegExpIPv4__Legacy["SeqHelper$SpecialRegExpIPv4"].has(arr, ele))
  }
  static len(s) {
    return runtime.safeCall(SpecialRegExpIPv4__Legacy["SeqHelper$SpecialRegExpIPv4"].len(s))
  }
  static lsEq(xs, ys) {
    let scrut11, x3, y1, xs4, ys2, scrut12, scrut13, element1_, element0_, arg_Cons_0_2, arg_Cons_1_2, arg_Cons_0_3, arg_Cons_1_3;
    scrut11 = ([
      xs,
      ys
    ]);
    element0_ = scrut11[0];
    element1_ = scrut11[1];
    if (element0_ instanceof SpecialRegExpIPv4.Nil) {
      if (element1_ instanceof SpecialRegExpIPv4.Nil) {
        return true
      }
      return false;
    } else if (element0_ instanceof SpecialRegExpIPv4.Cons.class) {
      arg_Cons_0_2 = element0_.x;
      arg_Cons_1_2 = element0_.xs;
      if (element1_ instanceof SpecialRegExpIPv4.Cons.class) {
        arg_Cons_0_3 = element1_.x;
        arg_Cons_1_3 = element1_.xs;
        ys2 = arg_Cons_1_3;
        y1 = arg_Cons_0_3;
        xs4 = arg_Cons_1_2;
        x3 = arg_Cons_0_2;
        scrut12 = runtime.safeCall(x3.eq(y1));
        if (scrut12 === true) {
          scrut13 = SpecialRegExpIPv4.lsEq(xs4, ys2);
          if (scrut13 === true) {
            return true
          }
          return false;
        }
        return false;
      }
      return false;
    }
    return false;
  }
  static match(p, s) {
    let p1, s1, acc, inlinedVal, scrut15, scrut16, c18, scrut17, scrut18, tmp34, tmp35, tmp36, tmp37;
    p1 = p;
    s1 = s;
    acc = "";
    tmp34 = SpecialRegExpIPv41.len(s1);
    scrut15 = tmp34 == 0;
    if (scrut15 === true) {
      scrut16 = runtime.safeCall(p1.canBeEmpty());
      if (scrut16 === true) {
        inlinedVal = (new SpecialRegExpIPv41.Some.class(acc));
        return inlinedVal
      }
      inlinedVal = (new SpecialRegExpIPv41.None());
      return inlinedVal;
    }
    if (p1 instanceof SpecialRegExpIPv41.Nothing.class) {
      inlinedVal = (new SpecialRegExpIPv41.None());
      return inlinedVal
    }
    c18 = s1[0];
    scrut17 = runtime.safeCall(p1.startsWith(c18));
    if (scrut17 === true) {
      tmp35 = runtime.safeCall(p1.derive(c18));
      tmp36 = runtime.safeCall(s1.slice(1));
      tmp37 = "" + c18;
      inlinedVal = SpecialRegExpIPv41.matchImpl(tmp35, tmp36, tmp37);
      return inlinedVal
    }
    scrut18 = runtime.safeCall(p1.canBeEmpty());
    if (scrut18 === true) {
      inlinedVal = (new SpecialRegExpIPv41.Some.class(acc));
      return inlinedVal
    }
    inlinedVal = (new SpecialRegExpIPv41.None());
    return inlinedVal;
  }
  static matchAll(p, s) {
    let tmp45, p1, s1, res, inlinedVal;
    tmp45 = ([]);
    p1 = p;
    s1 = s;
    res = tmp45;
    inlinedLbl: {
      loopLabel: while (true) {
        let scrut19, scrut20, ss, scrut21, tmp38, arg_Some_0_, tmp39, tmp40, tmp41, tmp42, tmp43, tmp44;
        tmp38 = SpecialRegExpIPv41.len(s1);
        scrut19 = tmp38 == 0;
        if (scrut19 === true) {
          inlinedVal = res;
          break inlinedLbl
        }
        scrut20 = SpecialRegExpIPv41.match(p1, s1);
        if (scrut20 instanceof SpecialRegExpIPv41.Some.class) {
          arg_Some_0_ = scrut20.x;
          ss = arg_Some_0_;
          tmp39 = SpecialRegExpIPv41.len(ss);
          scrut21 = tmp39 > 0;
          if (scrut21 === true) {
            tmp40 = SpecialRegExpIPv41.len(ss);
            tmp41 = runtime.safeCall(s1.slice(tmp40));
            tmp42 = runtime.safeCall(SpecialRegExpIPv4__Legacy["SeqHelper$SpecialRegExpIPv4"].push(res, ss));
            inlinedVal = SpecialRegExpIPv41.matchAllImpl(p1, tmp41, tmp42);
            break inlinedLbl
          }
          tmp43 = runtime.safeCall(s1.slice(1));
          s1 = tmp43;
          continue loopLabel;
        }
        tmp44 = runtime.safeCall(s1.slice(1));
        s1 = tmp44;
        continue loopLabel;
      }
    }
    return inlinedVal
  }
  static matchAllIPv4(s) {
    let fo, fi, segment, ipv4, tmp46, tmp47, tmp48, tmp49, tmp50, tmp51, tmp52, tmp53, tmp54, tmp55, tmp56, tmp57, tmp58, tmp59, tmp60, tmp61, tmp62, tmp63, tmp64, tmp65, tmp66, r, inlinedVal, r1, inlinedVal1, r2, inlinedVal2, p, s1, inlinedVal3, tmp45, p1, s2, res, inlinedVal4, tmp32, r3, inlinedVal5, tmp321, r4, inlinedVal6, tmp27, tmp271;
    tmp46 = ([
      "0",
      "1",
      "2",
      "3",
      "4"
    ]);
    fo = (new SpecialRegExpIPv4.In.class(tmp46));
    tmp47 = ([
      "0",
      "1",
      "2",
      "3",
      "4",
      "5"
    ]);
    fi = (new SpecialRegExpIPv4.In.class(tmp47));
    tmp48 = (new SpecialRegExpIPv4.Exact.class("2"));
    tmp49 = (new SpecialRegExpIPv4.Exact.class("5"));
    tmp50 = (new SpecialRegExpIPv4.Concat.class(tmp49, fi));
    tmp51 = (new SpecialRegExpIPv4.Concat.class(tmp48, tmp50));
    tmp52 = (new SpecialRegExpIPv4.Exact.class("2"));
    tmp53 = SpecialRegExpIPv4.digits();
    tmp54 = (new SpecialRegExpIPv4.Concat.class(fo, tmp53));
    tmp55 = (new SpecialRegExpIPv4.Concat.class(tmp52, tmp54));
    tmp56 = (new SpecialRegExpIPv4.Exact.class("1"));
    r = tmp56;
    tmp271 = (new SpecialRegExpIPv41.Empty.class());
    inlinedVal = (new SpecialRegExpIPv41.Altern.class(r, tmp271));
    tmp57 = inlinedVal;
    tmp58 = SpecialRegExpIPv4.digits();
    r1 = tmp58;
    tmp27 = (new SpecialRegExpIPv41.Empty.class());
    inlinedVal1 = (new SpecialRegExpIPv41.Altern.class(r1, tmp27));
    tmp59 = inlinedVal1;
    tmp60 = SpecialRegExpIPv4.digits();
    tmp61 = (new SpecialRegExpIPv4.Concat.class(tmp59, tmp60));
    tmp62 = (new SpecialRegExpIPv4.Concat.class(tmp57, tmp61));
    tmp63 = (new SpecialRegExpIPv4.Altern.class(tmp55, tmp62));
    segment = (new SpecialRegExpIPv4.Altern.class(tmp51, tmp63));
    tmp64 = (new SpecialRegExpIPv4.Exact.class("."));
    tmp65 = (new SpecialRegExpIPv4.Concat.class(segment, tmp64));
    r2 = tmp65;
    r3 = r2;
    r4 = r3;
    inlinedVal6 = r4;
    tmp321 = inlinedVal6;
    inlinedVal5 = (new SpecialRegExpIPv41.Concat.class(r3, tmp321));
    tmp32 = inlinedVal5;
    inlinedVal2 = (new SpecialRegExpIPv41.Concat.class(r2, tmp32));
    tmp66 = inlinedVal2;
    ipv4 = (new SpecialRegExpIPv4.Concat.class(tmp66, segment));
    p = ipv4;
    s1 = s;
    tmp45 = ([]);
    p1 = p;
    s2 = s1;
    res = tmp45;
    inlinedLbl: {
      loopLabel: while (true) {
        let scrut19, scrut20, ss, scrut21, tmp38, arg_Some_0_, tmp39, tmp40, tmp41, tmp42, tmp43, tmp44;
        tmp38 = SpecialRegExpIPv41.len(s2);
        scrut19 = tmp38 == 0;
        if (scrut19 === true) {
          inlinedVal4 = res;
          break inlinedLbl
        }
        scrut20 = match_SpecialRegExpIPv4_sp_0(p1, s2);
        if (scrut20 instanceof SpecialRegExpIPv41.Some.class) {
          arg_Some_0_ = scrut20.x;
          ss = arg_Some_0_;
          tmp39 = SpecialRegExpIPv41.len(ss);
          scrut21 = tmp39 > 0;
          if (scrut21 === true) {
            let p2, s3, res1, inlinedVal7;
            tmp40 = SpecialRegExpIPv41.len(ss);
            tmp41 = runtime.safeCall(s2.slice(tmp40));
            tmp42 = runtime.safeCall(SpecialRegExpIPv4__Legacy["SeqHelper$SpecialRegExpIPv4"].push(res, ss));
            p2 = p1;
            s3 = tmp41;
            res1 = tmp42;
            inlinedLbl1: {
              loopLabel1: while (true) {
                let scrut191, scrut201, ss1, scrut211, tmp381, arg_Some_0_1, tmp391, tmp401, tmp411, tmp421, tmp431, tmp441;
                tmp381 = SpecialRegExpIPv41.len(s3);
                scrut191 = tmp381 == 0;
                if (scrut191 === true) {
                  inlinedVal7 = res1;
                  break inlinedLbl1
                }
                scrut201 = match_SpecialRegExpIPv4_sp_0(p2, s3);
                if (scrut201 instanceof SpecialRegExpIPv41.Some.class) {
                  arg_Some_0_1 = scrut201.x;
                  ss1 = arg_Some_0_1;
                  tmp391 = SpecialRegExpIPv41.len(ss1);
                  scrut211 = tmp391 > 0;
                  if (scrut211 === true) {
                    tmp401 = SpecialRegExpIPv41.len(ss1);
                    tmp411 = runtime.safeCall(s3.slice(tmp401));
                    tmp421 = runtime.safeCall(SpecialRegExpIPv4__Legacy["SeqHelper$SpecialRegExpIPv4"].push(res1, ss1));
                    s3 = tmp411;
                    res1 = tmp421;
                    continue loopLabel1
                  }
                  tmp431 = runtime.safeCall(s3.slice(1));
                  s3 = tmp431;
                  continue loopLabel1;
                }
                tmp441 = runtime.safeCall(s3.slice(1));
                s3 = tmp441;
                continue loopLabel1;
              }
            }
            inlinedVal4 = inlinedVal7;
            break inlinedLbl
          }
          tmp43 = runtime.safeCall(s2.slice(1));
          s2 = tmp43;
          continue loopLabel;
        }
        tmp44 = runtime.safeCall(s2.slice(1));
        s2 = tmp44;
        continue loopLabel;
      }
    }
    inlinedVal3 = inlinedVal4;
    return inlinedVal3
  }
  static matchAllImpl(p, s, res) {
    loopLabel: while (true) {
      let scrut19, scrut20, ss, scrut21, tmp38, arg_Some_0_, tmp39, tmp40, tmp41, tmp42, tmp43, tmp44;
      tmp38 = SpecialRegExpIPv4.len(s);
      scrut19 = tmp38 == 0;
      if (scrut19 === true) {
        return res
      }
      scrut20 = SpecialRegExpIPv4.match(p, s);
      if (scrut20 instanceof SpecialRegExpIPv4.Some.class) {
        arg_Some_0_ = scrut20.x;
        ss = arg_Some_0_;
        tmp39 = SpecialRegExpIPv4.len(ss);
        scrut21 = tmp39 > 0;
        if (scrut21 === true) {
          tmp40 = SpecialRegExpIPv4.len(ss);
          tmp41 = runtime.safeCall(s.slice(tmp40));
          tmp42 = runtime.safeCall(SpecialRegExpIPv4__Legacy["SeqHelper$SpecialRegExpIPv4"].push(res, ss));
          s = tmp41;
          res = tmp42;
          continue loopLabel
        }
        tmp43 = runtime.safeCall(s.slice(1));
        s = tmp43;
        continue loopLabel;
      }
      tmp44 = runtime.safeCall(s.slice(1));
      s = tmp44;
      continue loopLabel;
    }
  }
  static matchImpl(p, s, acc) {
    loopLabel: while (true) {
      let scrut15, scrut16, c18, scrut17, scrut18, tmp34, tmp35, tmp36, tmp37;
      tmp34 = SpecialRegExpIPv4.len(s);
      scrut15 = tmp34 == 0;
      if (scrut15 === true) {
        scrut16 = runtime.safeCall(p.canBeEmpty());
        if (scrut16 === true) {
          return (new SpecialRegExpIPv4.Some.class(acc))
        }
        return (new SpecialRegExpIPv4.None());
      }
      if (p instanceof SpecialRegExpIPv4.Nothing.class) {
        return (new SpecialRegExpIPv4.None())
      }
      c18 = s[0];
      scrut17 = runtime.safeCall(p.startsWith(c18));
      if (scrut17 === true) {
        tmp35 = runtime.safeCall(p.derive(c18));
        tmp36 = runtime.safeCall(s.slice(1));
        tmp37 = acc + c18;
        p = tmp35;
        s = tmp36;
        acc = tmp37;
        continue loopLabel
      }
      scrut18 = runtime.safeCall(p.canBeEmpty());
      if (scrut18 === true) {
        return (new SpecialRegExpIPv4.Some.class(acc))
      }
      return (new SpecialRegExpIPv4.None());
    }
  }
  static mkUnion(ls) {
    let x4, x5, xs5, arg_Cons_0_4, arg_Cons_1_4, tmp26;
    if (ls instanceof SpecialRegExpIPv4.Cons.class) {
      arg_Cons_0_4 = ls.x;
      arg_Cons_1_4 = ls.xs;
      if (arg_Cons_1_4 instanceof SpecialRegExpIPv4.Nil) {
        x4 = arg_Cons_0_4;
        return x4
      }
      xs5 = arg_Cons_1_4;
      x5 = arg_Cons_0_4;
      tmp26 = SpecialRegExpIPv4.mkUnion(xs5);
      return (new SpecialRegExpIPv4.Altern.class(x5, tmp26));
    }
    return (new SpecialRegExpIPv4.Nothing.class());
  }
  static nTimes(r, i) {
    let scrut14, tmp31, tmp32;
    scrut14 = i == 1;
    if (scrut14 === true) {
      return r
    }
    tmp31 = i - 1;
    tmp32 = SpecialRegExpIPv4.nTimes(r, tmp31);
    return (new SpecialRegExpIPv4.Concat.class(r, tmp32));
  }
  static notDigit() {
    let tmp25, tup_3;
    tup_3 = ([
      "0",
      "1",
      "2",
      "3",
      "4",
      "5",
      "6",
      "7",
      "8",
      "9"
    ]);
    tmp25 = tup_3;
    return (new SpecialRegExpIPv4.Not.class(tmp25))
  }
  static notSpace() {
    let tmp24, tup_2;
    tup_2 = ([
      " ",
      "\n",
      "\t",
      "\r"
    ]);
    tmp24 = tup_2;
    return (new SpecialRegExpIPv4.Not.class(tmp24))
  }
  static notWord() {
    let tmp23, tup_1;
    tup_1 = ([
      "a",
      "b",
      "c",
      "d",
      "e",
      "f",
      "g",
      "h",
      "i",
      "j",
      "k",
      "l",
      "m",
      "n",
      "o",
      "p",
      "q",
      "r",
      "s",
      "t",
      "u",
      "v",
      "w",
      "x",
      "y",
      "z",
      "A",
      "B",
      "C",
      "D",
      "E",
      "F",
      "G",
      "H",
      "I",
      "J",
      "K",
      "L",
      "M",
      "N",
      "O",
      "P",
      "Q",
      "R",
      "S",
      "T",
      "U",
      "V",
      "W",
      "X",
      "Y",
      "Z"
    ]);
    tmp23 = tup_1;
    return (new SpecialRegExpIPv4.Not.class(tmp23))
  }
  static plus(r) {
    let tmp33;
    tmp33 = (new SpecialRegExpIPv4.Star.class(r));
    return (new SpecialRegExpIPv4.Concat.class(r, tmp33))
  }
  static push(arr, ele) {
    return runtime.safeCall(SpecialRegExpIPv4__Legacy["SeqHelper$SpecialRegExpIPv4"].push(arr, ele))
  }
  static pushUnique(ls, e) {
    let x2, xs1, scrut10, arg_Cons_0_, arg_Cons_1_, tmp21;
    if (ls instanceof SpecialRegExpIPv4.Cons.class) {
      arg_Cons_0_ = ls.x;
      arg_Cons_1_ = ls.xs;
      xs1 = arg_Cons_1_;
      x2 = arg_Cons_0_;
      scrut10 = runtime.safeCall(x2.eq(e));
      if (scrut10 === true) {
        return ls
      }
      tmp21 = SpecialRegExpIPv4.pushUnique(xs1, e);
      return (new SpecialRegExpIPv4.Cons.class(x2, tmp21));
    }
    return (new SpecialRegExpIPv4.Cons.class(e, SpecialRegExpIPv4.Nil));
  }
  static question(r) {
    let tmp27;
    tmp27 = (new SpecialRegExpIPv4.Empty.class());
    return (new SpecialRegExpIPv4.Altern.class(r, tmp27))
  }
  static spaces() {
    let tmp29, tup_5;
    tup_5 = ([
      " ",
      "\n",
      "\t",
      "\r"
    ]);
    tmp29 = tup_5;
    return (new SpecialRegExpIPv4.In.class(tmp29))
  }
  static words() {
    let tmp28, tup_4;
    tup_4 = ([
      "a",
      "b",
      "c",
      "d",
      "e",
      "f",
      "g",
      "h",
      "i",
      "j",
      "k",
      "l",
      "m",
      "n",
      "o",
      "p",
      "q",
      "r",
      "s",
      "t",
      "u",
      "v",
      "w",
      "x",
      "y",
      "z",
      "A",
      "B",
      "C",
      "D",
      "E",
      "F",
      "G",
      "H",
      "I",
      "J",
      "K",
      "L",
      "M",
      "N",
      "O",
      "P",
      "Q",
      "R",
      "S",
      "T",
      "U",
      "V",
      "W",
      "X",
      "Y",
      "Z"
    ]);
    tmp28 = tup_4;
    return (new SpecialRegExpIPv4.In.class(tmp28))
  }
  toString() { return runtime.render(this); }
  static [definitionMetadata] = ["class", "SpecialRegExpIPv4"];
});
let SpecialRegExpIPv4 = SpecialRegExpIPv41; export default SpecialRegExpIPv4;
