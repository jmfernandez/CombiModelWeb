#!/usr/bin/perl -w

#-- Combina modelos metabolicos
#-- Esta version incluye la busqueda y ajuste de reacciones de biomasa

#-- Javier Tamames, Mayo 2012
use strict;

use Data::Dumper;

use XML::LibXML::Reader;
use XML::Writer;
#use Carp qw();

########################
# Function definitions #
########################
sub extractReactions($$\%\%\%\%\%\%\%\%);
sub minmedia($$\%);
sub metab(\%\%\%);
sub reacs($$\%\%\%\%\%\%\%);
sub objective($$\%\%\%\%\%);
sub rxnDuplicadas(\%\%\%);
sub writeModel($\%\%\%\%\%\%);

{
$| = 1;

my %attTable = (); # Atributos (ph, coef) de cada modelo

my $minmediasw   = 0;    #-- A uno, ajusta el medio a la composicion de los medios minimos de cada componente
my $newobjective = 0;    #-- A uno, elimina las reacciones objetivo iniciales y crea una nueva
my $namemodel = '';
my $nummodel = 0;

my %ctoTable = (); # Compartimentos y la abb a usar
my %ctoName = (); # Nombres de compartimentos
my %joinreactions = ();
my %outcompart = ();
my %compounds = ();
my %reactions = ();
my %compreactions = ();
my %biomass = ();
my %joincompounds = ();
my %allcomparts = ();
my %joincompreactions = ();
my %minreac = ();

for my $models (@ARGV) {
	if ( $models eq '-minmedia' ) {
		$minmediasw = 1;
	} elsif ( $models eq '-newobj' ) {
		$newobjective = 1;
	} else {
		$nummodel++;
		$namemodel .= "$nummodel: $models; ";
		extractReactions( $models, $nummodel, %attTable, %ctoTable, %ctoName, %outcompart, %compounds , %reactions , %compreactions, %biomass );    #-- Lectura de modelos y almacenamiento de componentes
		if ($minmediasw) { minmedia( $models, $nummodel, %minreac); }    #-- Lectura de medios minimos (ya generados por minmedia.m)
	}
}

#-- Normalizacion de compuestos
metab(%compounds,%joincompounds,%allcomparts);
#-- Normalizacion de reacciones
reacs($newobjective,$minmediasw,%compounds,%reactions,%biomass,%compreactions,%joincompreactions,%minreac,%joinreactions);
#-- Aniade una nueva reaccion objetivo
objective($nummodel,$newobjective,%attTable,%biomass,%joincompounds,%joinreactions,%joincompreactions);

# Obtener identificadores de reacciones a eliminar (estan duplicadas)
my @todelete = rxnDuplicadas(%compounds,%compreactions,%minreac);
# Eliminar de %joinreactions
foreach my $id (@todelete) {
	delete $joinreactions{$id};
}

writeModel($namemodel,%allcomparts,%outcompart,%joincompounds,%joinreactions,%joincompreactions,%ctoName);                                               #-- Salida
exit(0);
}

########################
# Function definitions #
########################
sub extractReactions($$\%\%\%\%\%\%\%\%) {
	my($modelFile,$model,$p_attTable,$p_ctoTable,$p_ctoName,$p_outcompart,$p_compounds,$p_reactions,$p_compreactions, $p_biomass) = @_;
	
	if(my $MR = XML::LibXML::Reader->new(location => $modelFile)) {
		if($MR->read() && $MR->localName() eq 'sbml') {
			# Let's save this, so the read is focused on this namespace
			my $SBMLnamespace = $MR->namespaceURI();
			
			my $reactionId     = undef;
			my $reactionName   = "";
			my $reactorproduct = "";
			my %objectivereact = ();
			
			my $numUnknown = 0;
			
			# Recuperar atributos del modelo (o valores por defecto si no existen)
			my $ph = $MR->getAttribute('ph');
			$ph = "7.2"  unless(defined($ph));
			my $coef = $MR->getAttribute('coef');
			$coef = "1"  unless(defined($coef));

			$p_attTable->{$model}{PH} = $ph;
			$p_attTable->{$model}{COEF} = $coef;
			
			# Now, let's get the list of compartments, species, etc...
			my $pat = XML::LibXML::Pattern->new('//s:listOfCompartments/s:compartment|//s:listOfSpecies/s:species|//s:listOfReactions/s:reaction|//s:reaction/s:listOfReactants|//s:reaction/s:listOfProducts|//s:reaction/s:kineticLaw/s:listOfParameters',{ 's' => $SBMLnamespace });
			my $patsr = XML::LibXML::Pattern->new('./s:speciesReference',{ 's' => $SBMLnamespace });
			my $patpar = XML::LibXML::Pattern->new('./s:parameter',{ 's' => $SBMLnamespace });
			while($MR->nextPatternMatch($pat)) {
				my $localName = $MR->localName();
				
				if($localName eq 'compartment') {
		#			#my($ffw,$ssw)=split(/\_/,$1);
		#			#if($ssw) { $iwd=$ssw; } else { $iwd=$ffw; }
		#			$iwd = $1;print "iwd $iwd\n";
		#			$iwd =~ s/\_//g;print "iwd $iwd\n";
		#			$abcomp = substr( $iwd, 0, 1 );print "abcomp $abcomp\n";
		#			$abcomp =~ tr/A-Z/a-z/;print "abcomp $abcomp\n";
		#			$outcomp = substr( $2, 0, 1 );print "outcomp $outcomp\n";
		#			$outcomp =~ tr/A-Z/a-z/;print "outcomp $outcomp\n";
		#			if ( $abcomp ne "e" ) {
		#				$incm = "$abcomp\_$model";print "incm $incm\n";
		#				if   ( $outcomp ne "e" ) { $outcm = "$outcomp\_$model";print "outcm $outcm\n"; }
		#				else                     { $outcm = $outcomp;print "outcm $outcm\n"; }
		#				$p_outcompart->{$incm} = $outcm;
		#			}
		#			print Dumper {%outcompart};

					#-- MODIFICACION PDSANCHEZ 20/12/2012
					# Modificar para evitar errores por compartimentos del tipo C_e C_c
					my $ctoId = $MR->getAttribute('id');
					my $ctoName = $MR->getAttribute('name');
					$ctoName = $ctoId  unless(defined($ctoName));
					my $abb;
					if ($ctoId =~ /ext|out|^e$|_e$/i) {
						$abb = "e";
					}
					elsif ($ctoId =~ /cytosol|^c$|_c$|int/i) {
						$abb = "c_$model";
					}
					#-- MODIFICACION PDSANCHEZ 02/2016
					# i --> intracelular => crear c
					elsif ($ctoId =~ /^i$|int/i) {
						$abb = "c_$model";
						$p_ctoTable->{$model}{"c"}{ABB} = $abb;
					}
					#-- FIN MODIFICACION 02/2016
					else {
						# There could be more than one unknown
						$abb = "x${numUnknown}_${model}";
						$numUnknown++;
					}
					$p_ctoTable->{$model}{$ctoId}{ABB} = $abb;
					$p_ctoName->{$model}{$abb} = $ctoName;
					
					my $outside = $MR->getAttribute('outside');
					if (defined($outside)) {
						if ($outside =~ /ext|out|^e$|_e$/i) {
							$p_outcompart->{$abb} = "e";
						} else {
							$p_outcompart->{$abb} = "$outside\_$model";
						}
					} else {
						$p_outcompart->{$abb} = undef;
					}
					#-- FIN MODIFICACION PDSANCHEZ 20/12/2012
				} elsif($localName eq 'species') {
					# Do it only once!
					if($MR->nodeType() == XML::LibXML::Reader::XML_READER_TYPE_ELEMENT) {
						my $coid    = $MR->getAttribute('id');
						my $coname  = $MR->getAttribute('name');
						my $compart = $MR->getAttribute('compartment');
						my $tcharge = $MR->getAttribute('charge');
						my $tboun   = $MR->getAttribute('boundaryCondition');

						# if($model>0) { print "$_\n"; }
						#Sacamos formula quimica del nombre
						my $compoundFormula = "";
						if(defined($coname)) {
							my @cp_parts = split( /\_/, $coname );
							foreach my $cp_part (@cp_parts) {
								$compoundFormula = $cp_part;
							}
						}

						#-- MODIFICACION PDSANCHEZ 20/12/2012
			#			#  my($ffw,$ssw)=split(/\_/,$compart);
			#			#  if($ssw) { $iwd=$ssw; } else { $iwd=$ffw; }
			#			$iwd = $compart;
			#			$iwd =~ s/\_//g;
			#
			#			$abcompart = substr( $iwd, 0, 1 );
			#			$abcompart =~ tr/A-Z/a-z/;

						my $abcompart = $p_ctoTable->{$model}{$compart}{ABB};
						#-- FIN MODIFICACION PDSANCHEZ 20/12/2012

						# my $crea =$dbh->do("INSERT INTO compounds values(\"$1\",\"$compoundFormula\",\"$3\",\"$model\",\"$2\",\"$4\",\"$5\");");
						$p_compounds->{$coid}{$model}{formula}     = $compoundFormula;
						$p_compounds->{$coid}{$model}{compartment} = $abcompart;
						$p_compounds->{$coid}{$model}{name}        = $coname;
						$p_compounds->{$coid}{$model}{charge}      = $tcharge;
						$p_compounds->{$coid}{$model}{boundary}    = $tboun;
						# print "*$coid*$model*$coname*$p_compounds->{$1}{$model}{compartment}*\n";
					}
				} elsif($localName eq 'reaction') {
					# Do it only once!
					if($MR->nodeType() == XML::LibXML::Reader::XML_READER_TYPE_ELEMENT) {
						$reactionId                                    = $MR->getAttribute('id');
						$reactionName                                  = $MR->getAttribute('name');
						$p_reactions->{$reactionId}{$model}{reactionname}  = $reactionName;
						$p_reactions->{$reactionId}{$model}{reversibility} = $MR->getAttribute('reversible');
					} elsif($MR->nodeType() == XML::LibXML::Reader::XML_READER_TYPE_END_ELEMENT) {
						if(exists($objectivereact{$reactionId})) {
							#Carp::cluck("RA $reactionId")  if(defined($reactionId));
							foreach my $prods ( keys %{ $p_compreactions->{$reactionId}{$model}{product} } ) {
								my $comname = $p_compounds->{$prods}{$model}{name};

								#-- MODIFICACION PDSANCHEZ 18/12/2012
								# Guardar id en lugar del nombre
								#if ( $comname =~ /biomass/i ) { $p_biomass->{$model} = $comname; }
								if ( $comname =~ /biomass/i ) {
									$p_biomass->{$model}{product} = $prods;

									my $prd = $prods;
									$prd =~ s/_[a-z]$/_b/;
									if (exists $p_compounds->{$prd}) {
										$p_biomass->{UNIQUE_ID}{$prd}++;
									}
									else {
										$p_biomass->{NON_UNIQUE_ID}{$prods}++;
									}
									$p_biomass->{RX_ID}{$reactionId."_".$model}++;
								}
								#-- FIN MODIFICACION PDSANCHEZ 18/12/2012

							}
							#-- MODIFICACION PDSANCHEZ 18/12/2012
							# Incluir reactantes
							foreach my $react ( keys %{ $p_compreactions->{$reactionId}{$model}{react} } ) {
								my $comname = $p_compounds->{$react}{$model}{name};

								#-- MODIFICACION PDSANCHEZ 18/12/2012
								# Guardar id en lugar del nombre
								#if ( $comname =~ /biomass/i ) { $p_biomass->{$model} = $comname; }
								if ( $comname =~ /biomass/i ) {
									$p_biomass->{$model}{react} = $react;

									my $prd = $react;
									$prd =~ s/_[a-z]$/_b/;
									if (exists $p_compounds->{$prd}) {
										$p_biomass->{UNIQUE_ID}{$prd}++;
									}
									else {
										$p_biomass->{NON_UNIQUE_ID}{$react}++;
									}
									$p_biomass->{RX_ID}{$reactionId."_".$model}++;
								}
								#-- FIN MODIFICACION PDSANCHEZ 18/12/2012

							}

							#-- FIN MODIFICACION PDSANCHEZ 18/12/2012

							if (!exists($p_biomass->{$model})) {                     #-- Si no lo hay, se aniade
							  #-- MODIFICACION PDSANCHEZ 18/12/2012
							  # Crear el mismo id para todos los modelos
								#my $bterm                                               = "biomass$model\_c";
								#$p_biomass->{$model}                                     = $bterm;
								#$p_compreactions->{$reactionId}{$model}{product}{$bterm} = 1;
								#$p_compounds->{$bterm}{$model}{compartment}              = "c";
								#$p_compounds->{$bterm}{$model}{name}                     = $bterm;
								#$p_compounds->{$bterm}{$model}{charge}                   = 0;
								#$p_compounds->{$bterm}{$model}{boundary}                 = "false";

								my $bterm                                               = "biomass_c";
								$p_biomass->{$model}                                     = $bterm;
								$p_compreactions->{$reactionId}{$model}{product}{$bterm} = 1;
								$p_compounds->{$bterm}{$model}{compartment}              = "c";
								$p_compounds->{$bterm}{$model}{name}                     = "biomass$model\_c";
								$p_compounds->{$bterm}{$model}{charge}                   = 0;
								$p_compounds->{$bterm}{$model}{boundary}                 = "false";
								$p_biomass->{NON_UNIQUE_ID}{$bterm}++;
								#-- FIN MODIFICACION PDSANCHEZ 18/12/2012
							}
						}
						
						$reactionId     = undef;
						$reactionName   = "";
						$reactorproduct = "";
					}
				} elsif($localName eq 'listOfReactants' || $localName eq 'listOfProducts') {
					my $reactorproduct = $localName eq 'listOfReactants' ? 'react' : 'product';
					if($MR->nextPatternMatch($patsr)) {
						do {
							$p_compreactions->{$reactionId}{$model}{$reactorproduct}{$MR->getAttribute('species')} = $MR->getAttribute('stoichiometry');
						} while($MR->nextSiblingElement('speciesReference',$SBMLnamespace));
					}
				} elsif($localName eq 'listOfParameters') {
					if($MR->nextPatternMatch($patpar)) {
						do {
							my $parameter_id = $MR->getAttribute('id');
							my $parameter_value = $MR->getAttribute('value');
							if    ( $parameter_id =~ /LOWER_BOUND/ ) { $p_reactions->{$reactionId}{$model}{lowerbound} = $parameter_value; }
							elsif ( $parameter_id =~ /UPPER_BOUND/ ) { $p_reactions->{$reactionId}{$model}{upperbound} = $parameter_value; }
							elsif ( $parameter_id =~ /OBJECTIVE_COEFFICIENT/ ) {
								$p_reactions->{$reactionId}{$model}{objectcoef} = $parameter_value;
								if ( $parameter_value > 0 ) {
									$objectivereact{$reactionId} = $parameter_value;
								}
							}
							elsif ( $parameter_id =~ /FLUX_VALUE/ ) {
								$p_reactions->{$reactionId}{$model}{fluxvalue} = $parameter_value;
							}
						} while($MR->nextSiblingElement('parameter',$SBMLnamespace));
					}
				}
			}
			#print Dumper {%{$p_biomass}};
		}
		$MR->finish();
	} else {
		die "ERROR: Unable to extract reactions from $modelFile\n";
	}
}

sub metab(\%\%\%) {
	my($p_compounds,$p_joincompounds,$p_allcomparts)=@_;
	
	foreach my $comps ( sort keys %{$p_compounds} ) {
		foreach my $inmodel ( sort keys %{ $p_compounds->{$comps} } ) {
			my $newcompart = $p_compounds->{$comps}{$inmodel}{compartment};
			my $newid;
			#-- MODIFICACION PDSANCHEZ 20/12/2012
			if   ( $p_compounds->{$comps}{$inmodel}{compartment} ne "e" ) {
				$newid = "$comps\_$inmodel";
				#$newcompart = "$p_compounds->{$comps}{$inmodel}{compartment}\_$inmodel";
			}
			else {
				$newid = $comps;
				#$newcompart = "$p_compounds->{$comps}{$inmodel}{compartment}";
			}
			#-- FIN MODIFICACION PDSANCHEZ 20/12/2012

			# $newid="$comps\_$inmodel";
			$p_joincompounds->{$newid}{compartment} = $newcompart;
			$p_joincompounds->{$newid}{formula}     = $p_compounds->{$comps}{$inmodel}{formula};
			$p_joincompounds->{$newid}{name}        = $p_compounds->{$comps}{$inmodel}{name};
			$p_joincompounds->{$newid}{charge}      = $p_compounds->{$comps}{$inmodel}{charge};
			$p_joincompounds->{$newid}{boundary}    = $p_compounds->{$comps}{$inmodel}{boundary};
			$p_allcomparts->{$newcompart}++;
		}

		#-- Faltan por ajustar las boundaries para extracelulares, tomara las del ultimo leido
	}
}

sub reacs($$\%\%\%\%\%\%\%) {
	my($newobjective,$minmediasw,$p_compounds,$p_reactions,$p_biomass,$p_compreactions,$p_joincompreactions,$p_minreac,$p_joinreactions) = @_;
	foreach my $reac ( sort keys %{$p_reactions} ) {
		foreach my $inmodel ( sort keys %{ $p_reactions->{$reac} } ) {
			my $generic = 1;
			foreach my $prods ( sort keys %{ $p_compreactions->{$reac}{$inmodel} } ) {
				foreach my $compinrec ( sort keys %{ $p_compreactions->{$reac}{$inmodel}{$prods} } ) {
					if ( $p_compounds->{$compinrec}{$inmodel}{compartment} ne "e" ) { $generic = 0; }
				}
			}
			my $newid;
			if   ( !$generic ) { $newid = "$reac\_$inmodel"; }
			else               { $newid = "$reac"; }             #-- Si todos los componenetes son extracelulares, es una reaccion generica

			foreach my $prods ( sort keys %{ $p_compreactions->{$reac}{$inmodel} } ) {    #-- Analiza los componentes de la reaccion
				foreach my $compinrec ( sort keys %{ $p_compreactions->{$reac}{$inmodel}{$prods} } ) {
					my $newidmet;
					if ( !$p_compounds->{$compinrec}{$inmodel}{compartment} ) { die "ERROR: Cannot find chemical species $compinrec in model $inmodel\n"; }
					elsif ( $p_compounds->{$compinrec}{$inmodel}{compartment} ne "e" ) {
						$newidmet = "$compinrec\_$inmodel";
						$generic  = 0;
					}
					else {
						$newidmet = $compinrec;
					}
					$p_joincompreactions->{$newid}{$prods}{$newidmet} = $p_compreactions->{$reac}{$inmodel}{$prods}{$compinrec};

					#  print " *$newid $prods\t$newidmet ($inmodel) $p_compounds->{$compinrec}{$inmodel}{compartment}\n";
				}
			}

			#-- MODIFICACION PDSANCHEZ 10/12/2012
			#-- Identificar las reacciones de intercambio con el universo
			$generic = 0;
			foreach my $prods ( sort keys %{ $p_compreactions->{$reac}{$inmodel} } ) {
				foreach my $compinrec ( sort keys %{ $p_compreactions->{$reac}{$inmodel}{$prods} } ) {
					if (defined($p_compounds->{$compinrec}{$inmodel}{boundary}) && $p_compounds->{$compinrec}{$inmodel}{boundary} eq "true" ) { $generic = 1; }
				}
			}

			#print " *$generic > $newid -- $newname\n";
			#-- FIN MODIFICACION PDSANCHEZ 10/12/2012

			my $newname;
			my $lowerb;
			my $upperb;
			if ( !$generic ) {
				$newid   = "$reac\_$inmodel";
				$newname = $p_reactions->{$reac}{$inmodel}{reactionname} . '_' . $inmodel;
				$lowerb  = $p_reactions->{$reac}{$inmodel}{lowerbound};
				$upperb  = $p_reactions->{$reac}{$inmodel}{upperbound};
			}
			else {
				$newid   = "$reac";
				$newname = $p_reactions->{$reac}{$inmodel}{reactionname};

				#-- Sera una reaccion de intercambio, hay que mirar si esta en el medio minimo
				if ( !$minmediasw ) {    #-- No hay que ajustar medios
					$lowerb = $p_reactions->{$reac}{$inmodel}{lowerbound};
					$upperb = $p_reactions->{$reac}{$inmodel}{upperbound};
				}
				else {                   #-- Hay que ajustar le medio minimo
					if ( $p_minreac->{$newid} ) {
						$lowerb = $p_reactions->{$reac}{$inmodel}{lowerbound};
						$upperb = $p_reactions->{$reac}{$inmodel}{upperbound};

						# A veces aparece el LB con valor cero. Poner -1000 para permitir entrada del compuesto
						$lowerb = ($lowerb == 0) ? "-1000" : $lowerb;
					}                      #-- Esta en el medio minimo de alguna de las especies
					else {
						$lowerb = 0;

						#-- MODIFICACION PDSANCHEZ 10/12/2012
						#-- Quitar UB=0 (no permite salida de compuestos, se acumulan y el crecimiento es nulo)
						#$upperb=0;
						#-- Se deja el valor de UB de los modelos
						$upperb = $p_reactions->{$reac}{$inmodel}{upperbound};
						#-- FIN MODIFICACION PDSANCHEZ 10/12/2012
					}
				}
				#if ( $newobjective && ( $reac =~ /EX\_cpd11416\_c/ ) ) { $lowerb = 0; $upperb = 0; }    #-- Es una reaccion de intercambio de biomasa, la vamos a anular

			}

			# print "$reac\t$inmodel -> $newid\n";
			$p_joinreactions->{$newid}{reactionname}  = $newname;
			$p_joinreactions->{$newid}{reversibility} = $p_reactions->{$reac}{$inmodel}{reversibility};
			$p_joinreactions->{$newid}{lowerbound}    = $lowerb;
			$p_joinreactions->{$newid}{upperbound}    = $upperb;

			#  $p_joinreactions->{$newid}{upperbound}=$p_reactions->{$reac}{$inmodel}{upperbound};

			if   ( !$newobjective ) {
				$p_joinreactions->{$newid}{objectcoef} = $p_reactions->{$reac}{$inmodel}{objectcoef};
			}
			else {
				# Poner cero en todas las func objetivo
				$p_joinreactions->{$newid}{objectcoef} = 0;

				#-- MODIFICACION PDSANCHEZ 19/12/2012
				# Poner LB y UB a cero en las func objetivo
				foreach my $rxid (keys %{$p_biomass->{RX_ID}}) {
					if ($rxid eq $newid) {
						$p_joinreactions->{$rxid}{lowerbound} = 0;
						$p_joinreactions->{$rxid}{upperbound} = 0;
					}
				}
				#-- FIN MODIFICACION PDSANCHEZ 19/12/2012
			}  #-- Vamos a aniadir una nueva funcion objetivo

			$p_joinreactions->{$newid}{fluxvalue} = $p_reactions->{$reac}{$inmodel}{fluxvalue};
		}
	}
}

sub objective($$\%\%\%\%\%) {
	my($numm,$newobjective,$p_attTable,$p_biomass,$p_joincompounds,$p_joinreactions,$p_joincompreactions) = @_;
	my $bcomp;

  #-- MODIFICACION PDSANCHEZ 19/12/2012
  # Cambiar "TotBiomass_e" por el identificador comun de la biomasa
	#my $bcomp = "TotBiomass_e";  #-- Aniade reaccion de generacion de biomasa total
	if (exists $p_biomass->{UNIQUE_ID}) {
		$bcomp = [keys %{$p_biomass->{UNIQUE_ID}}]->[0];
	}
	elsif(exists $p_biomass->{NON_UNIQUE_ID}) {
		$bcomp = [keys %{$p_biomass->{NON_UNIQUE_ID}}]->[0];
	} else {
		$bcomp = "TotBiomass_e";
	}
	$bcomp =~ s/_[a-z]$/_e/;  #-- Aniade reaccion de generacion de biomasa total
	#-- FIN MODIFICACION PDSANCHEZ 19/12/2012

	my $breac = "Generation_Biomass_total";
	$p_joincompounds->{$bcomp}{compartment}   = "e";
	$p_joincompounds->{$bcomp}{formula}       = "";
	$p_joincompounds->{$bcomp}{name}          = "Biomass_total";
	$p_joincompounds->{$bcomp}{charge}        = 0;
	$p_joincompounds->{$bcomp}{boundary}      = "false";
	$p_joinreactions->{$breac}{reactionname}  = $breac;
	$p_joinreactions->{$breac}{reversibility} = "false";
	#$p_joinreactions->{$breac}{lowerbound}    = 0;
	#$p_joinreactions->{$breac}{upperbound}    = 1000;
	if($newobjective) {
		$p_joinreactions->{$breac}{objectcoef} = 1;
		$p_joinreactions->{$breac}{lowerbound} = 0;
		$p_joinreactions->{$breac}{upperbound} = 1000;
	} else {
		$p_joinreactions->{$breac}{objectcoef} = 0;
		$p_joinreactions->{$breac}{lowerbound} = 0;
		$p_joinreactions->{$breac}{upperbound} = 0;
	}
	$p_joinreactions->{$breac}{fluxvalue} = 0;

	#-- MODIFICACION PDSANCHEZ 19/12/2012
	for ( my $nm = 1 ; $nm <= $numm ; $nm++ ) {
		# $biom = "$p_biomass->{$nm}\_$nm";
		my $biom = $p_biomass->{$nm}{react}.'_'.$nm;
		#$p_joincompreactions->{$breac}{react}{$biom} = 1;

		$p_joincompreactions->{$breac}{react}{$biom} = $p_attTable->{$nm}{COEF};
	}
	#-- MODIFICACION PDSANCHEZ 19/12/2012

	$p_joincompreactions->{$breac}{product}{$bcomp} = 1;


  #-- MODIFICACION PDSANCHEZ 19/12/2012
  # Cambiar "TotBiomass_b" por el identificador comun de la biomasa
	#my $bcompb = "TotBiomass_b";    #-- Aniade reaccion de intercambio de biomasa total (necesaria para que haya flujo)
	#my $bcompb;
	#if (exists $p_biomass->{UNIQUE_ID}) {
	#	$bcompb = [keys %{$p_biomass->{UNIQUE_ID}}]->[0];
	#}
	#else {
	#	$bcompb = [keys %{$p_biomass->{NON_UNIQUE_ID}}]->[0];
	#	$bcompb =~ s/_[a-z]$/_b/;  #-- Aniade reaccion de intercambio de biomasa total (necesaria para que haya flujo)
	#}
  #
  # Esto no es necesario ya que duplicariamos el numero de reacciones
	#my $breac  = "EX_TotBiomass";
	#$p_joincompounds->{$bcompb}{compartment}         = "e";
	#$p_joincompounds->{$bcompb}{formula}             = "";
	#$p_joincompounds->{$bcompb}{name}                = "Biomass_total";
	#$p_joincompounds->{$bcompb}{charge}              = 0;
	#$p_joincompounds->{$bcompb}{boundary}            = "true";
	#$p_joinreactions->{$breac}{reactionname}         = $breac;
	#$p_joinreactions->{$breac}{reversibility}        = "false";
	#$p_joinreactions->{$breac}{lowerbound}           = 0;
	#$p_joinreactions->{$breac}{upperbound}           = 1000;
	#$p_joinreactions->{$breac}{objectcoef}           = 0;
	#$p_joinreactions->{$breac}{fluxvalue}            = 0;
	#$p_joincompreactions->{$breac}{react}{$bcomp}    = 1;
	#$p_joincompreactions->{$breac}{product}{$bcompb} = 1;
	#-- FIN MODIFICACION PDSANCHEZ 19/12/2012

#  print Dumper {%{$p_biomass}};
#	#print Dumper {%{$p_joinreactions}};
#	foreach my $id (keys %{$p_joinreactions}) {
#		my $name = $p_joinreactions->{$id}{reactionname};
#		if ($name =~ /biomass/i) {
#			print "\n--> $id\n";
#			print Dumper {%{$p_joinreactions->{$id}}};
#		}
#	}

}

use constant DEFAULT_UNITS => 'mmol_per_gDW_per_hr';
use constant OUTPUT_SBML_NS => 'http://www.sbml.org/sbml/level2';
use constant XHTML_NS => 'http://www.w3.org/1999/xhtml';
use constant MATHML_NS => 'http://www.w3.org/1998/Math/MathML';

sub writeModel($\%\%\%\%\%\%) {
	my($namemodel,$p_allcomparts,$p_outcompart,$p_joincompounds,$p_joinreactions,$p_joincompreactions,$p_ctoName) = @_;
	
	#Encabezados
	my $MW = XML::Writer->new(
		NAMESPACES => 1,
		#NEWLINES => 1,
		ENCODING => 'utf-8',
		PREFIX_MAP => {
			OUTPUT_SBML_NS() => '',
			XHTML_NS() => 'html',
			MATHML_NS() => 'math'
		}
	);
	$MW->xmlDecl('UTF-8');
	$MW->startTag([OUTPUT_SBML_NS,'sbml'],'level' => '2','version' => '1');$MW->characters("\n");
	$MW->startTag([OUTPUT_SBML_NS,'model'],'id' => 'Combined','name' => $namemodel);$MW->characters("\n");
	
	#Unidades de medida

	$MW->startTag([OUTPUT_SBML_NS,'listOfUnitDefinitions']);$MW->characters("\n");
		$MW->startTag([OUTPUT_SBML_NS,'unitDefinition'],'id' => DEFAULT_UNITS);$MW->characters("\n");
			$MW->startTag([OUTPUT_SBML_NS,'listOfUnits']);$MW->characters("\n");
				$MW->emptyTag([OUTPUT_SBML_NS,'unit'],'kind' => 'mole','scale' => '-3');$MW->characters("\n");
				$MW->emptyTag([OUTPUT_SBML_NS,'unit'],'kind' => 'gram','exponent' => '-1');$MW->characters("\n");
				$MW->emptyTag([OUTPUT_SBML_NS,'unit'],'kind' => 'second','multiplier' => '.00027777','exponent' => '-1');$MW->characters("\n");
			$MW->endTag();$MW->characters("\n");	# listOfUnits
		$MW->endTag();$MW->characters("\n");	# unitDefinition
	$MW->endTag();$MW->characters("\n");	# listOfUnitDefinitions
	
	#Primero formamos los compartimentos
	$MW->startTag([OUTPUT_SBML_NS,'listOfCompartments']);$MW->characters("\n");
	foreach my $compl ( sort keys %{$p_allcomparts} ) {
		my $outcmp = $p_outcompart->{$compl};
		my @idl = split( /\_/, $compl );
		my $compname;
		$compname = $p_ctoName->{$idl[1]}{$compl}  if(scalar(@idl)>1);
		unless(defined($compname)) {
			if    ( $idl[0] eq "c" ) { $compname = "Cytosol"; }
			elsif ( $idl[0] eq "p" ) { $compname = "Periplasm"; }
			elsif ( $idl[0] eq "e" ) { $compname = "Extracellular"; }
			else { $compname = "Unknown"; }
		}
		if ( defined($idl[1]) ) { $compname .= "\_$idl[1]"; }
		
		my @attrs = (
			'id'	=> $compl,
			'name'	=> $compname
		);
		if ($outcmp) { push(@attrs,'outside' => $outcmp); }
		$MW->emptyTag([OUTPUT_SBML_NS,'compartment'],@attrs);$MW->characters("\n");
	}
	$MW->endTag();$MW->characters("\n");	# listOfCompartments

	#Lista de especies

	$MW->startTag([OUTPUT_SBML_NS,'listOfSpecies']);$MW->characters("\n");
	foreach my $tcomp ( sort keys %{$p_joincompounds} ) {
		my @attrs = (
			'id'	=> $tcomp,
			'compartment'	=> $p_joincompounds->{$tcomp}{compartment},
		);
		push(@attrs,'name' => $p_joincompounds->{$tcomp}{name})  if(defined($p_joincompounds->{$tcomp}{name}));
		push(@attrs,'boundaryCondition' => $p_joincompounds->{$tcomp}{boundary})  if(defined($p_joincompounds->{$tcomp}{boundary}));
		push(@attrs,'charge' => $p_joincompounds->{$tcomp}{charge})  if(defined($p_joincompounds->{$tcomp}{charge}));
		$MW->emptyTag([OUTPUT_SBML_NS,'species'],@attrs);$MW->characters("\n");
	}
	$MW->endTag();$MW->characters("\n");	# listOfSpecies

	#Reacciones
	$MW->startTag([OUTPUT_SBML_NS,'listOfReactions']);$MW->characters("\n");
	foreach my $react ( sort keys %{$p_joinreactions} ) {
		my @reactAttrs = (
			'id'	=> $react,
			'name'	=> $p_joinreactions->{$react}{reactionname},
		);
		push(@reactAttrs,'reversible' => $p_joinreactions->{$react}{reversibility})  if(defined($p_joinreactions->{$react}{reversibility}));
		
		my $plb = $p_joinreactions->{$react}{lowerbound};
		my $pub = $p_joinreactions->{$react}{upperbound};
		my $objCoef = $p_joinreactions->{$react}{objectcoef};
		my $fluxValue = $p_joinreactions->{$react}{fluxvalue};
		
		if($p_joincompreactions->{$react}{react} || $p_joincompreactions->{$react}{product} || defined($plb) || defined($pub) || defined($objCoef) || defined($fluxValue)) {
			$MW->startTag([OUTPUT_SBML_NS,'reaction'],@reactAttrs);$MW->characters("\n");
			if ( $p_joincompreactions->{$react}{react} ) {
				
				$MW->startTag([OUTPUT_SBML_NS,'listOfReactants']);$MW->characters("\n");
				foreach my $reactant ( sort keys %{ $p_joincompreactions->{$react}{react} } ) {
					my @spReference = (
						'species'	=> $reactant,
					);
					push(@spReference,'stoichiometry' => $p_joincompreactions->{$react}{react}{$reactant})  if(defined($p_joincompreactions->{$react}{react}{$reactant}));
					$MW->emptyTag([OUTPUT_SBML_NS,'speciesReference'],@spReference);$MW->characters("\n");
				}
				$MW->endTag();$MW->characters("\n");	# listOfReactants
			}
			if ( $p_joincompreactions->{$react}{product} ) {
				$MW->startTag([OUTPUT_SBML_NS,'listOfProducts']);$MW->characters("\n");
				foreach my $product ( sort keys %{ $p_joincompreactions->{$react}{product} } ) {
					my @spReference = (
						'species'	=> $product,
					);
					push(@spReference,'stoichiometry' => $p_joincompreactions->{$react}{product}{$product})  if(defined($p_joincompreactions->{$react}{product}{$product}));
					$MW->emptyTag([OUTPUT_SBML_NS,'speciesReference'],@spReference);$MW->characters("\n");
				}
				$MW->endTag();$MW->characters("\n");	# listOfProducts
			}
			
			# We generate a kinetic law only when there is something interesting to show there
			if(defined($plb) || defined($pub) || defined($objCoef) || defined($fluxValue)) {
				$MW->startTag([OUTPUT_SBML_NS,'kineticLaw']);$MW->characters("\n");
					### $plb = ($plb < -100) ? -100 : $plb; #-- para poner a -100 valores inferiores (ej -1000)
					### $pub = ($pub > 100) ? 100 : $pub; #-- para poner a 100 valores superiores (ej 1000)
					if(defined($plb)) {
						$plb = ($plb < 0 && $plb > -100) ? -1000 : $plb; #-- para incremetar el flujo cuando es muy bajito
					}
					if(defined($pub)) {
						$pub = ($pub > 0 && $pub < 100) ? 1000 : $pub; #-- para incremetar el flujo cuando es muy bajito
					}
					$MW->startTag([MATHML_NS,'math']);$MW->characters("\n");
						$MW->startTag([MATHML_NS,'apply']);$MW->characters("\n");
						if(defined($plb)) {
							$MW->dataElement([MATHML_NS,'ci'],' LOWER_BOUND ');$MW->characters("\n");
						}
						if(defined($pub)) {
							$MW->dataElement([MATHML_NS,'ci'],' UPPER_BOUND ');$MW->characters("\n");
						}
						if(defined($objCoef)) {
							$MW->dataElement([MATHML_NS,'ci'],' OBJECTIVE_COEFFICIENT ');$MW->characters("\n");
						}
						if(defined($fluxValue)) {
							$MW->dataElement([MATHML_NS,'ci'],' FLUX_VALUE ');$MW->characters("\n");
						}
						$MW->endTag();$MW->characters("\n");	# apply
					$MW->endTag();$MW->characters("\n");	# math
					$MW->startTag([OUTPUT_SBML_NS,'listOfParameters']);$MW->characters("\n");
					
					if(defined($plb)) {
						$MW->emptyTag([OUTPUT_SBML_NS,'parameter'],'id' => 'LOWER_BOUND','value' => $plb,'units' => DEFAULT_UNITS);$MW->characters("\n");
						#$MW->emptyTag([OUTPUT_SBML_NS,'parameter'],'id' => 'LOWER_BOUND','value' => $p_joinreactions->{$react}{lowerbound},'units' => DEFAULT_UNITS);
					}
					if(defined($pub)) {
						$MW->emptyTag([OUTPUT_SBML_NS,'parameter'],'id' => 'UPPER_BOUND','value' => $pub,'units' => DEFAULT_UNITS);$MW->characters("\n");
						#$MW->emptyTag([OUTPUT_SBML_NS,'parameter'],'id' => 'UPPER_BOUND','value' => $p_joinreactions->{$react}{upperbound},'units' => DEFAULT_UNITS);
					}
						
					if(defined($objCoef)) {
						$MW->emptyTag([OUTPUT_SBML_NS,'parameter'],'id' => 'OBJECTIVE_COEFFICIENT','value' => $objCoef);$MW->characters("\n");
					}
					if(defined($fluxValue)) {
						$MW->emptyTag([OUTPUT_SBML_NS,'parameter'],'id' => 'FLUX_VALUE','value' => $fluxValue,'units' => DEFAULT_UNITS);$MW->characters("\n");
					}
					$MW->endTag();$MW->characters("\n");	# listOfParameters
				$MW->endTag();$MW->characters("\n");	# kineticLaw
			}
			$MW->endTag();$MW->characters("\n");	# reaction
		} else {
			$MW->emptyTag([OUTPUT_SBML_NS,'reaction'],@reactAttrs);$MW->characters("\n");
		}
	}
	$MW->endTag();$MW->characters("\n");	# listOfReactions
	
	
	$MW->endTag();$MW->characters("\n");	# model
	$MW->endTag();	# sbml
	$MW->end();
}

sub minmedia($$\%) {
	my($minmedia,$numm,$p_minreac) = @_;
	
	my $minmediaOrig = $minmedia;
	$minmedia =~ s/xml/minmedia_ul/;
	if(open(my $IN, '<', $minmedia)) {
		while (my $line = <$IN>) {
			chomp($line);
			my( $ex, $namer ) = split( /\t/, $line );
			$ex =~ s/\(e\)/\_e/g;
			$p_minreac->{$ex}{$numm} = $namer;
		}
		close($IN);
	} else {
		die "Cannot read minimum media $minmedia from $minmediaOrig\n";
	}
}

sub rxnDuplicadas(\%\%\%) {
	my($p_compounds,$p_compreactions,$p_minreac) = @_;
	
	my %table = ();
	foreach my $id (keys %{$p_compreactions}) {
		my $isbd = 0;
		foreach my $md (keys %{$p_compreactions->{$id}}) {
			my @lst = ();
			foreach my $rc (sort keys %{$p_compreactions->{$id}{$md}{react}}) {
				if (defined($p_compounds->{$rc}{$md}{boundary}) && $p_compounds->{$rc}{$md}{boundary} eq "true") {
					$isbd = 1;
				}
				push(@lst, $rc);
			}
			foreach my $pd (sort keys %{$p_compreactions->{$id}{$md}{product}}) {
				if (defined($p_compounds->{$pd}{$md}{boundary}) && $p_compounds->{$pd}{$md}{boundary} eq "true") {
					$isbd = 1;
				}
				push(@lst, $pd);
			}
			if ($isbd) {
				$table{join("|", @lst)}{$id} = $md;
			}
		}
	}

	my @todelete = ();
	foreach my $key (keys %table) {
		my @a = keys %{$table{$key}};
		my $t = scalar @a;
		if ($t > 1) {
			my $inmm = 0;
			my @eliminar = ();
			foreach my $id (@a) {
				# Si aparece en mmin se respeta
				if (exists $p_minreac->{$id}) {
					$inmm++;
				}
				else {
					# No aparece, la guardo para eliminar
					push(@eliminar, $id)
				}
			}

			# Si todo esta en mm cojo uno
			if ($t == $inmm) {
				push(@eliminar, pop(@a));
			}

			# Si esta en el mm elimino todo lo de @eliminar
			# Si no esta elimino todo lo de eliminar menos un valor
			unless ($inmm) {
				# Saco un elemento (que respetare) y el resto lo elimino
				pop(@eliminar);
			}
			push(@todelete, @eliminar);
		}
	}

	return @todelete;
}
