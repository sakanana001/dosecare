"""重现舍曲林 50mg q24h 浓度曲线, 看 cMax 实际值"""
import math
import sys
sys.path.insert(0, r'C:\Users\yuwen\Desktop\精品神药\app\src\main\java\com\dosecare\app\domain\pk')

# 直接手算一房室浓度
# 舍曲林: ka=1.5, ke=ln(2)/26, F=0.5, Vd=20*70=1400, dose=50mg
ka = 1.5
ke = math.log(2) / 26  # 0.0267
F = 0.5
Vd = 20.0 * 70
dose = 50.0
tau = 24.0

# Css_avg = F * D / (CL * tau) = F * D / (ke * Vd * tau)
CL = ke * Vd
Css_avg = F * dose / (CL * tau)
print(f"CL = {CL:.2f} L/h, Css_avg = {Css_avg*1000:.2f} ng/mL")

# Css_max (steady state peak) = Css_avg * (1 - exp(-ke*tau)) * (exp(-ke*Tmax))
Tmax = math.log(ka / ke) / (ka - ke)
print(f"Tmax = {Tmax:.2f} h")
Css_max = (F * dose * ka) / (Vd * (ka - ke)) * (math.exp(-ke * Tmax) - math.exp(-ka * Tmax))
print(f"Css_max = {Css_max*1000:.2f} ng/mL")

# Simulate 5 days q24h
def c_one_comp(t, dose_time, dose_mg):
    """t 时刻浓度, 多 dose_time 列表"""
    total = 0
    for dt in dose_time:
        tau_i = t - dt
        if tau_i < 0:
            continue
        if abs(ka - ke) < 1e-9:
            total += F * dose_mg * ka * tau_i * math.exp(-ke * tau_i) / Vd
        else:
            coeff = (F * dose_mg * ka) / (Vd * (ka - ke))
            total += coeff * (math.exp(-ke * tau_i) - math.exp(-ka * tau_i))
    return total

doses_at = [i * tau for i in range(10)]  # 0, 24, 48, ..., 216
print(f"\nDoses at: {doses_at[:6]}...")
print(f"\n时间(h) | 浓度(ng/mL)")
for t in range(0, 121, 4):
    c = c_one_comp(t, doses_at, dose) * 1000  # to ng/mL
    print(f"  {t:4d}  | {c:8.2f}")
