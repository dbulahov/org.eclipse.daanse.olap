/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.testkit.function;

import java.util.List;

import org.eclipse.daanse.olap.testkit.function.contracts.AbsContract;
import org.eclipse.daanse.olap.testkit.function.contracts.AddCalculatedMembersContract;
import org.eclipse.daanse.olap.testkit.function.contracts.AggregateContract;
import org.eclipse.daanse.olap.testkit.function.contracts.AllMembersContract;
import org.eclipse.daanse.olap.testkit.function.contracts.AncestorContract;
import org.eclipse.daanse.olap.testkit.function.contracts.AncestorsContract;
import org.eclipse.daanse.olap.testkit.function.contracts.AsContract;
import org.eclipse.daanse.olap.testkit.function.contracts.AscendantsContract;
import org.eclipse.daanse.olap.testkit.function.contracts.AvgContract;
import org.eclipse.daanse.olap.testkit.function.contracts.BottomCountContract;
import org.eclipse.daanse.olap.testkit.function.contracts.BottomPercentContract;
import org.eclipse.daanse.olap.testkit.function.contracts.BottomSumContract;
import org.eclipse.daanse.olap.testkit.function.contracts.CachedExistsContract;
import org.eclipse.daanse.olap.testkit.function.contracts.CalculatedChildContract;
import org.eclipse.daanse.olap.testkit.function.contracts.CaptionContract;
import org.eclipse.daanse.olap.testkit.function.contracts.ChildrenContract;
import org.eclipse.daanse.olap.testkit.function.contracts.ClosingPeriodContract;
import org.eclipse.daanse.olap.testkit.function.contracts.CorrelationContract;
import org.eclipse.daanse.olap.testkit.function.contracts.CousinContract;
import org.eclipse.daanse.olap.testkit.function.contracts.CovarianceContract;
import org.eclipse.daanse.olap.testkit.function.contracts.CovarianceNContract;
import org.eclipse.daanse.olap.testkit.function.contracts.CrossjoinContract;
import org.eclipse.daanse.olap.testkit.function.contracts.CurrentContract;
import org.eclipse.daanse.olap.testkit.function.contracts.CurrentMemberContract;
import org.eclipse.daanse.olap.testkit.function.contracts.CurrentOrdinalContract;
import org.eclipse.daanse.olap.testkit.function.contracts.DataMemberContract;
import org.eclipse.daanse.olap.testkit.function.contracts.DefaultMemberContract;
import org.eclipse.daanse.olap.testkit.function.contracts.DescendantsContract;
import org.eclipse.daanse.olap.testkit.function.contracts.DimensionContract;
import org.eclipse.daanse.olap.testkit.function.contracts.DimensionsContract;
import org.eclipse.daanse.olap.testkit.function.contracts.DistinctContract;
import org.eclipse.daanse.olap.testkit.function.contracts.DrilldownLevelBottomContract;
import org.eclipse.daanse.olap.testkit.function.contracts.DrilldownLevelContract;
import org.eclipse.daanse.olap.testkit.function.contracts.DrilldownLevelTopContract;
import org.eclipse.daanse.olap.testkit.function.contracts.DrilldownMemberContract;
import org.eclipse.daanse.olap.testkit.function.contracts.ExceptContract;
import org.eclipse.daanse.olap.testkit.function.contracts.ExistingContract;
import org.eclipse.daanse.olap.testkit.function.contracts.ExistsContract;
import org.eclipse.daanse.olap.testkit.function.contracts.ExtractContract;
import org.eclipse.daanse.olap.testkit.function.contracts.FilterContract;
import org.eclipse.daanse.olap.testkit.function.contracts.FirstChildContract;
import org.eclipse.daanse.olap.testkit.function.contracts.FirstQContract;
import org.eclipse.daanse.olap.testkit.function.contracts.FirstSiblingContract;
import org.eclipse.daanse.olap.testkit.function.contracts.FormatContract;
import org.eclipse.daanse.olap.testkit.function.contracts.FunctionContract;
import org.eclipse.daanse.olap.testkit.function.contracts.GenerateContract;
import org.eclipse.daanse.olap.testkit.function.contracts.HeadContract;
import org.eclipse.daanse.olap.testkit.function.contracts.HierarchizeContract;
import org.eclipse.daanse.olap.testkit.function.contracts.HierarchyContract;
import org.eclipse.daanse.olap.testkit.function.contracts.ItemContract;
import org.eclipse.daanse.olap.testkit.function.contracts.KPICurrentTimeMemberContract;
import org.eclipse.daanse.olap.testkit.function.contracts.KPIGoalContract;
import org.eclipse.daanse.olap.testkit.function.contracts.KPIStatusContract;
import org.eclipse.daanse.olap.testkit.function.contracts.KPITrendContract;
import org.eclipse.daanse.olap.testkit.function.contracts.KPIValueContract;
import org.eclipse.daanse.olap.testkit.function.contracts.KPIWeightContract;
import org.eclipse.daanse.olap.testkit.function.contracts.LagContract;
import org.eclipse.daanse.olap.testkit.function.contracts.LastChildContract;
import org.eclipse.daanse.olap.testkit.function.contracts.LastPeriodsContract;
import org.eclipse.daanse.olap.testkit.function.contracts.LastSiblingContract;
import org.eclipse.daanse.olap.testkit.function.contracts.LeadContract;
import org.eclipse.daanse.olap.testkit.function.contracts.LenContract;
import org.eclipse.daanse.olap.testkit.function.contracts.LevelContract;
import org.eclipse.daanse.olap.testkit.function.contracts.LevelNumberContract;
import org.eclipse.daanse.olap.testkit.function.contracts.LinRegInterceptContract;
import org.eclipse.daanse.olap.testkit.function.contracts.LinRegPointContract;
import org.eclipse.daanse.olap.testkit.function.contracts.LinRegR2Contract;
import org.eclipse.daanse.olap.testkit.function.contracts.LinRegSlopeContract;
import org.eclipse.daanse.olap.testkit.function.contracts.LinRegVarianceContract;
import org.eclipse.daanse.olap.testkit.function.contracts.MaxContract;
import org.eclipse.daanse.olap.testkit.function.contracts.MedianContract;
import org.eclipse.daanse.olap.testkit.function.contracts.MemberCaptionContract;
import org.eclipse.daanse.olap.testkit.function.contracts.MemberUniqueNameContract;
import org.eclipse.daanse.olap.testkit.function.contracts.MembersContract;
import org.eclipse.daanse.olap.testkit.function.contracts.MembersFunctionContract;
import org.eclipse.daanse.olap.testkit.function.contracts.MinContract;
import org.eclipse.daanse.olap.testkit.function.contracts.MinusContract;
import org.eclipse.daanse.olap.testkit.function.contracts.MtdContract;
import org.eclipse.daanse.olap.testkit.function.contracts.NameContract;
import org.eclipse.daanse.olap.testkit.function.contracts.NativizeSetContract;
import org.eclipse.daanse.olap.testkit.function.contracts.NextMemberContract;
import org.eclipse.daanse.olap.testkit.function.contracts.NonEmptyContract;
import org.eclipse.daanse.olap.testkit.function.contracts.NonEmptyCrossJoinContract;
import org.eclipse.daanse.olap.testkit.function.contracts.OpeningPeriodContract;
import org.eclipse.daanse.olap.testkit.function.contracts.OrderContract;
import org.eclipse.daanse.olap.testkit.function.contracts.OrderKeyContract;
import org.eclipse.daanse.olap.testkit.function.contracts.OrdinalContract;
import org.eclipse.daanse.olap.testkit.function.contracts.ParallelPeriodContract;
import org.eclipse.daanse.olap.testkit.function.contracts.ParamRefContract;
import org.eclipse.daanse.olap.testkit.function.contracts.ParameterContract;
import org.eclipse.daanse.olap.testkit.function.contracts.ParentContract;
import org.eclipse.daanse.olap.testkit.function.contracts.PercentileContract;
import org.eclipse.daanse.olap.testkit.function.contracts.PeriodsToDateContract;
import org.eclipse.daanse.olap.testkit.function.contracts.PrevMemberContract;
import org.eclipse.daanse.olap.testkit.function.contracts.PropertiesContract;
import org.eclipse.daanse.olap.testkit.function.contracts.QtdContract;
import org.eclipse.daanse.olap.testkit.function.contracts.RangeContract;
import org.eclipse.daanse.olap.testkit.function.contracts.SetToStrContract;
import org.eclipse.daanse.olap.testkit.function.contracts.SiblingsContract;
import org.eclipse.daanse.olap.testkit.function.contracts.StarContract;
import org.eclipse.daanse.olap.testkit.function.contracts.StddevContract;
import org.eclipse.daanse.olap.testkit.function.contracts.StddevPContract;
import org.eclipse.daanse.olap.testkit.function.contracts.StdevContract;
import org.eclipse.daanse.olap.testkit.function.contracts.StdevPContract;
import org.eclipse.daanse.olap.testkit.function.contracts.StrContract;
import org.eclipse.daanse.olap.testkit.function.contracts.StrToMemberContract;
import org.eclipse.daanse.olap.testkit.function.contracts.StripCalculatedMembersContract;
import org.eclipse.daanse.olap.testkit.function.contracts.SubsetContract;
import org.eclipse.daanse.olap.testkit.function.contracts.SumContract;
import org.eclipse.daanse.olap.testkit.function.contracts.TailContract;
import org.eclipse.daanse.olap.testkit.function.contracts.ToggleDrillStateContract;
import org.eclipse.daanse.olap.testkit.function.contracts.TopCountContract;
import org.eclipse.daanse.olap.testkit.function.contracts.TopPercentContract;
import org.eclipse.daanse.olap.testkit.function.contracts.TopSumContract;
import org.eclipse.daanse.olap.testkit.function.contracts.TupleToStrContract;
import org.eclipse.daanse.olap.testkit.function.contracts.UCaseContract;
import org.eclipse.daanse.olap.testkit.function.contracts.UnionContract;
import org.eclipse.daanse.olap.testkit.function.contracts.UniqueNameContract;
import org.eclipse.daanse.olap.testkit.function.contracts.UnorderContract;
import org.eclipse.daanse.olap.testkit.function.contracts.ValidMeasureContract;
import org.eclipse.daanse.olap.testkit.function.contracts.ValueContract;
import org.eclipse.daanse.olap.testkit.function.contracts.WtdContract;
import org.eclipse.daanse.olap.testkit.function.contracts.YtdContract;

/**
 * The enumeration of all contracts. Hand-maintained, exactly like
 * {@code StandardFunctions.standard()} — and for the same reason: subclasses cannot be
 * enumerated without classpath scanning, and there is no scanner in the build.
 */
public final class FunctionContracts {

    private FunctionContracts() {
    }

    public static List<FunctionContract> all() {
        return List.of(
                AbsContract.CONTRACT,
                AddCalculatedMembersContract.CONTRACT,
                AggregateContract.CONTRACT,
                AllMembersContract.CONTRACT,
                AncestorContract.CONTRACT,
                AncestorsContract.CONTRACT,
                AsContract.CONTRACT,
                AscendantsContract.CONTRACT,
                AvgContract.CONTRACT,
                BottomCountContract.CONTRACT,
                BottomPercentContract.CONTRACT,
                BottomSumContract.CONTRACT,
                CachedExistsContract.CONTRACT,
                CalculatedChildContract.CONTRACT,
                CaptionContract.CONTRACT,
                ChildrenContract.CONTRACT,
                ClosingPeriodContract.CONTRACT,
                CorrelationContract.CONTRACT,
                CousinContract.CONTRACT,
                CovarianceContract.CONTRACT,
                CovarianceNContract.CONTRACT,
                CrossjoinContract.CONTRACT,
                CurrentContract.CONTRACT,
                CurrentMemberContract.CONTRACT,
                CurrentOrdinalContract.CONTRACT,
                DataMemberContract.CONTRACT,
                DefaultMemberContract.CONTRACT,
                DescendantsContract.CONTRACT,
                DimensionContract.CONTRACT,
                DimensionsContract.CONTRACT,
                DistinctContract.CONTRACT,
                DrilldownLevelBottomContract.CONTRACT,
                DrilldownLevelContract.CONTRACT,
                DrilldownLevelTopContract.CONTRACT,
                DrilldownMemberContract.CONTRACT,
                ExceptContract.CONTRACT,
                ExistingContract.CONTRACT,
                ExistsContract.CONTRACT,
                ExtractContract.CONTRACT,
                FilterContract.CONTRACT,
                FirstChildContract.CONTRACT,
                FirstQContract.CONTRACT,
                FirstSiblingContract.CONTRACT,
                FormatContract.CONTRACT,
                GenerateContract.CONTRACT,
                HeadContract.CONTRACT,
                HierarchizeContract.CONTRACT,
                HierarchyContract.CONTRACT,
                ItemContract.CONTRACT,
                KPICurrentTimeMemberContract.CONTRACT,
                KPIGoalContract.CONTRACT,
                KPIStatusContract.CONTRACT,
                KPITrendContract.CONTRACT,
                KPIValueContract.CONTRACT,
                KPIWeightContract.CONTRACT,
                LagContract.CONTRACT,
                LastChildContract.CONTRACT,
                LastPeriodsContract.CONTRACT,
                LastSiblingContract.CONTRACT,
                LeadContract.CONTRACT,
                LenContract.CONTRACT,
                LevelContract.CONTRACT,
                LevelNumberContract.CONTRACT,
                LinRegInterceptContract.CONTRACT,
                LinRegPointContract.CONTRACT,
                LinRegR2Contract.CONTRACT,
                LinRegSlopeContract.CONTRACT,
                LinRegVarianceContract.CONTRACT,
                MaxContract.CONTRACT,
                MedianContract.CONTRACT,
                MemberCaptionContract.CONTRACT,
                MemberUniqueNameContract.CONTRACT,
                MembersContract.CONTRACT,
                MembersFunctionContract.CONTRACT,
                MinContract.CONTRACT,
                MinusContract.CONTRACT,
                MtdContract.CONTRACT,
                NameContract.CONTRACT,
                NativizeSetContract.CONTRACT,
                NextMemberContract.CONTRACT,
                NonEmptyContract.CONTRACT,
                NonEmptyCrossJoinContract.CONTRACT,
                OpeningPeriodContract.CONTRACT,
                OrderContract.CONTRACT,
                OrderKeyContract.CONTRACT,
                OrdinalContract.CONTRACT,
                ParallelPeriodContract.CONTRACT,
                ParamRefContract.CONTRACT,
                ParameterContract.CONTRACT,
                ParentContract.CONTRACT,
                PercentileContract.CONTRACT,
                PeriodsToDateContract.CONTRACT,
                PrevMemberContract.CONTRACT,
                PropertiesContract.CONTRACT,
                QtdContract.CONTRACT,
                RangeContract.CONTRACT,
                SetToStrContract.CONTRACT,
                SiblingsContract.CONTRACT,
                StarContract.CONTRACT,
                StddevContract.CONTRACT,
                StddevPContract.CONTRACT,
                StdevContract.CONTRACT,
                StdevPContract.CONTRACT,
                StrContract.CONTRACT,
                StrToMemberContract.CONTRACT,
                StripCalculatedMembersContract.CONTRACT,
                SubsetContract.CONTRACT,
                SumContract.CONTRACT,
                TailContract.CONTRACT,
                ToggleDrillStateContract.CONTRACT,
                TopCountContract.CONTRACT,
                TopPercentContract.CONTRACT,
                TopSumContract.CONTRACT,
                TupleToStrContract.CONTRACT,
                UCaseContract.CONTRACT,
                UnionContract.CONTRACT,
                UniqueNameContract.CONTRACT,
                UnorderContract.CONTRACT,
                ValidMeasureContract.CONTRACT,
                ValueContract.CONTRACT,
                WtdContract.CONTRACT,
                YtdContract.CONTRACT
                // ... one line per function
        );
    }
}